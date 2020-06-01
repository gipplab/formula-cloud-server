const { Client } = require('@elastic/elasticsearch');
const crypto = require('crypto');
const md5Hash = crypto.createHash('md5');

const chunkSize = 10_000;

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
}

ESConnector.prototype.close = function() {
    client.close();
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

ESConnector.prototype._updateChunk = async function (dataArray) {
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
 * @param docID {String}
 */
ESConnector.prototype.updateIndex = function(mathElements, docID) {
    if ( this._updateCache.length < chunkSize ) {
        this._updateCache.push(this._buildMOIs(mathElements, docID));
    } else {
        let updateArray = this._updateCache.splice(0, chunkSize);
        this._updateCache.push(this._buildMOIs(mathElements, docID));
        this._updateChunk(updateArray)
            .then((resp) => {
                console.log("Updated chunk of docs.");
                console.log(resp);
                updateArray.splice(0, updateArray.length);
                updateArray = undefined;
            })
            .catch((err) => {
                console.error("Unable to update chunk of documents with MOIs: " + err);
                console.error(err);
            });
    }
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
