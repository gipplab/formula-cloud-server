const { Client } = require('@elastic/elasticsearch');
const crypto = require('crypto');
const md5Hash = crypto.createHash('md5');

const chunkSize = 5_000;

const client = new Client({
    node: 'http://localhost:9200',
    log: 'info'
});

const _scrollRequestParams = {
    scroll: '30s',
    size: 1_000,
    _source: ['title', 'database'],
    body: {
        query: {
            bool: {
                must_not: [
                    {
                        nested: {
                            path: "moi",
                            query: {
                                match_all: {}
                            }
                        }
                    }
                ]
            }
        }
    }
}

let index = undefined;

let ESConnector = function( options ) {
    this._options = options;
    this._updateCache = [];
    this._updateFidsCache = {};
    this._currentSize = 0;
}

ESConnector.prototype.close = async function() {
    return client.close();
}

ESConnector.prototype.setIndex = function (newIndex) {
    index = newIndex;
    _scrollRequestParams['index'] = newIndex;
}

ESConnector.prototype._scrollSearch = client.helpers.scrollDocuments({
        index: index,
        _source: ['title', 'database'],
        body: {
            query: {
                // match all to match all, regardless of existing information
                // match_all: {}
                // or we only take the entries that do not contain mois yet
                bool: {
                    must_not: [
                        {
                            nested: {
                                path: "moi",
                                query: {
                                    match_all: {}
                                }
                            }
                        }
                    ]
                }
            }
        }
    });

ESConnector.prototype._manualScrollSearch = async function * () {
    let response = await client.search(_scrollRequestParams);

    while ( true ) {
        const sourceHits = response.body.hits.hits;

        if ( sourceHits.length === 0 ){
            break;
        }

        for ( const hit of sourceHits ) {
            yield hit._source;
        }

        if ( !response.body._scroll_id ){
            break;
        }

        response = await client.scroll({
            scrollId: response.body._scroll_id,
            scroll: '30s'
        });
    }
}

ESConnector.prototype._addMOIToText = async function (dataArray) {
    return client.helpers.bulk({
        datasource: dataArray,
        concurrency: 8,
        onDocument (doc) {
            return [
                {
                    update: {
                        _index: index,
                        _id: doc.docID
                    }
                },
                {
                    doc: {
                        moi: doc.mathElements
                    }
                }
            ]
        }
    });
}

ESConnector.prototype._addMOIToTextManualBulk = async function(dataArray) {
    const body = dataArray.flatMap(element => [
        {
            update: {
                _index: index,
                _id: element.docID
            }
        },
        {
            doc: {
                moi: element.mathElements
            }
        }
    ]);

    return client.bulk({
        refresh: true,
        body
    });
}

ESConnector.prototype._addMOIFormulaIDs = async function (data) {
    const body = Object.keys(data).flatMap(moiMD5 => [
        {
            update: {
                _index: index,
                _id: moiMD5
            }
        },
        {
            script: {
                source:
                    "if (ctx._source.formulaIDs == null) {" +
                        "ctx._source.formulaIDs = []" +
                    "}" +
                    "for ( fid in params.newIDs ) {" +
                        "if (!ctx._source.formulaIDs.contains(fid)) {" +
                            "ctx._source.formulaIDs.add(fid)" +
                        "}" +
                    "}",
                params: {
                    newIDs: data[moiMD5]
                }
            }
        }
    ]);

    return client.bulk({
        refresh: true,
        body
    });
}

ESConnector.prototype.loadDocumentIDs = async function(callback) {
    return new Promise(async (resolve, reject) => {
        console.log("Let's scroll documents");

        // for await (const doc of this._scrollSearch) {
        for await (const doc of this._manualScrollSearch()) {
            await callback(doc);
        }

        resolve();
    });
}

ESConnector.prototype._buildMOIs = function(mathElements, docID) {
    return {
        docID: docID,
        mathElements: mathElements.map(mathElement => {
            return {
                moiMD5: md5Hash.copy().update(mathElement.expression).digest('base64'),
                localTermFrequency: mathElement.termFrequency
            }
        })
    }
}

/**
 *
 * @param mathElements {Array<MathElement>}
 * @private
 */
ESConnector.prototype._updateCacheWithFIDs = function(mathElements) {
    mathElements.map(m => {
        return {
            moiMD5: md5Hash.copy().update(m.expression).digest('base64'),
            formulaIDs: m.formulaIDs
        }
    }).forEach(m => {
        if ( this._updateFidsCache[m.moiMD5] ) {
            m.formulaIDs.forEach(mFID => {
                if ( !this._updateFidsCache[m.moiMD5].includes(mFID) )
                    this._updateFidsCache[m.moiMD5] += mFID + ' ';
            });
        } else {
            this._updateFidsCache[m.moiMD5] = [...m.formulaIDs].join(' ');
        }

        // if ( this._updateFidsCache[m.moiMD5] ) {
        //     m.formulaIDs.forEach(mFID => {
        //         this._updateFidsCache[m.moiMD5].add(mFID)
        //     });
        // } else {
        //     this._updateFidsCache[m.moiMD5] = new Set(m.formulaIDs);
        // }
        this._currentSize += m.formulaIDs.length;
    })
}

/**
 *
 * @param mathElements {Array<MathElement>}
 * @param docID {String}
 */
ESConnector.prototype.addMOIToTextIndex = function(mathElements, docID) {
    if ( this._updateCache.length < chunkSize ) {
        this._updateCache.push(this._buildMOIs(mathElements, docID));
    } else {
        let updateArray = this._updateCache.splice(0, chunkSize);
        this._updateCache.push(this._buildMOIs(mathElements, docID));
        this._addMOIToTextManualBulk(updateArray)
            .then((resp) => {
                let respBody = resp.body;
                if ( respBody.errors ) {
                    console.log("Errors occurred during bulk operation. Listing errors...")
                    respBody.items.forEach((item, idx) => {
                        if ( item.update && item.update.error ) {
                            console.log("Item " + idx + " failed because: " + item.update.error.reason);
                        }
                    });
                } else {
                    console.log("Updated chunk of docs.");
                }
                updateArray.splice(0, updateArray.length);
                updateArray = undefined;
            })
            .catch((err) => {
                console.error("Unable to update chunk of documents with MOIs: " + err);
                console.error(err);
            });
    }
}

ESConnector.prototype.flushMOIToTextMemory = async function() {
    return this._addMOIToText(this._updateCache)
        .then((resp) => {
            console.log("Updated chunk of docs.");
            console.log(resp);
        })
        .catch((err) => {
            console.error("Unable to update chunk of documents with MOIs: " + err);
            console.error(err);
        });
}

ESConnector.prototype.flushUpdates = async function() {
    if ( this._updateCache.length > 0 ) {
        await this._updateChunk(this._updateCache)
            .then((resp) => {
                this._updateCache = [];
                console.log(resp);
            }).catch((err) => {
                console.error("Unable to flush last updates. " + err);
            });
    }
}

module.exports = ESConnector = {
    ESConnector: ESConnector,
}
