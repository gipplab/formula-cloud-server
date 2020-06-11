const crypto = require('crypto');
const md5Hash = crypto.createHash('md5');

let Cache = function() {
    this._updateFidsCache = {};
    this._currentSize = 0;
}

/**
 *
 * @param mathElements {Array<MathElement>}
 * @private
 */
Cache.prototype._updateCacheWithFIDs = function(mathElements) {
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
 * @returns {Promise<void>}
 */
Cache.prototype.addFormulaIDsToMOI = async function(mathElements) {
    if ( this._currentSize % 100_000 < 50 ) {
        console.log("\nCurrent size: " + this._currentSize);
        console.log("Number of elements in cache: " + Object.keys(this._updateFidsCache).length);
    }

    // if ( this._currentSize < chunkSize/2 && Object.keys(this._updateFidsCache).length < 2_000 ) {
    this._updateCacheWithFIDs(mathElements);
    // } else {
    //     this._updateCacheWithFIDs(mathElements);
    //     await this._addMOIFormulaIDs(this._updateFidsCache)
    //         .then((success) => {
    //             console.log("\nSuccessfully updated " + success.body.items.length + " MOIs.");
    //         }).catch((error) => {
    //             console.error("An error occurred when updating the indices: " + error);
    //             console.error(error);
    //         });
    //     this._updateFidsCache = {}; // reset
    //     this._currentSize = 0;
    // }
}

Cache.prototype.getFormulaCache = function () {
    return this._updateFidsCache
}

module.exports = {
    Cache: Cache
}