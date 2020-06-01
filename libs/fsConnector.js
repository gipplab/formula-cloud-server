const BB = require('bluebird');
const fs = BB.promisifyAll(require('fs'));
const LineByLineReader = require('line-by-line');
const path = require('path');

let FSConnector = function (filePath, skipLines) {
    this._filepath = filePath;
    this._skipLines = skipLines;
}

FSConnector.prototype.getDocIds = async function (callback) {
    return new Promise((resolve, reject) => {
        console.log("Start reading file " + this._filepath);
        let lineReader = new LineByLineReader(this._filepath, {encoding: 'utf8', skipEmptyLines: true});
        let counter = 0;
        const skipLines = this._skipLines;
        if ( skipLines > 0 ) {
            console.log("Skipping first " + skipLines + " lines.");
        }

        lineReader.on('line', async function(line) {
            // first skip the lines we already processed.
            lineReader.pause();
            if (counter < skipLines) {
                counter++;
                lineReader.resume();
                return;
            }

            let group = line.match(/^(\d+?),/m);
            let docID = group[1];
            counter++;
            await callback({title: docID});
            lineReader.resume();
        });

        lineReader.on('error', function(err) {
            console.error('An error occurred when reading lines.');
            console.error(err);
            reject(err);
        });

        lineReader.on('end', function() {
            console.log("Read all lines from file. Total number " + counter);
            resolve(counter);
        });
    })
}

module.exports = FSConnector = {
    FSConnector: FSConnector
}