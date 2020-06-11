const BB = require('bluebird');
const fs = BB.promisifyAll(require('fs'));
const LineByLineReader = require('line-by-line');
const path = require('path');
const moiConsole = require('./moi-console');

let FsConnector = function (filePath, skipLines) {
    this._filepath = filePath;
    this._skipLines = skipLines;
    this._globalCounter = 0;
}

FsConnector.prototype._getDocIDObject = function (line) {
    let group = line.match(/^(\d+?),/m);
    let docID = group[1];
    return {title: docID};
}

FsConnector.prototype._getMathElement = function (line) {
    let groups = line.match(/^"(.*)";(\d+);(\d+);(\d+)$/m);
    let moi = groups[1];
    let depth = groups[2];
    let tf = groups[3];
    let df = groups[4];

    return {
        moi: moi,
        complexity: depth,
        tf: tf,
        df: df
    }
}

FsConnector.prototype._loadData = async function(filePath, callback, lineHandler) {
    return new Promise((resolve, reject) => {
        console.log("Start reading file " + filePath);
        let lineReader = new LineByLineReader(filePath, {encoding: 'utf8', skipEmptyLines: true});
        let counter = 0;
        const skipLines = this._skipLines;
        if ( skipLines > 0 ) {
            console.log("Skipping first " + skipLines + " lines.");
        }

        lineReader.on('line', async function(line) {
            // first skip the lines we already processed.
            lineReader.pause();
            if (counter < skipLines) {
                this._globalCounter++;
                counter++;
                lineReader.resume();
                return;
            }

            let object = lineHandler(line);
            this._globalCounter++;
            counter++;
            await callback(object);
            lineReader.resume();
        });

        lineReader.on('error', function(err) {
            console.error('An error occurred when reading lines.');
            console.error(err);
            reject(err);
        });

        lineReader.on('end', function() {
            console.log("Read all lines from file " + filePath + "; Lines: " + counter + "; Total lines: " + this._globalCounter);
            resolve(counter);
        });
    })
}

FsConnector.prototype.getDocIds = async function (callback) {
    return this._loadData(this._filepath, callback, this._getDocIDObject);
}

FsConnector.prototype._deep = async function (dir, callback) {
    return fs.readdirAsync(dir)
        .map(function (name) {
            const stats = fs.statSync(path.join(dir, name));

            if ( stats.isDirectory() ){
                return deep(path.join(dir,name));
            } else {
                return path.join(dir, name);
            }
        })
        .filter(function(res) {
            return res != null;

        })
        .map(async (file) => {
            await this._loadData(file, callback, this._getMathElement);
        });
}

FsConnector.prototype._processFilesInDir = async function(databaseName, dir, bib) {
    const self = this;
    bib[databaseName] = [];
    return new Promise((resolve, reject) => {
        fs.readdirAsync(dir)
            .filter((file) => {
                const stats = fs.statSync(path.join(dir, file));
                return !stats.isDirectory();
            })
            .map((file) => {
                let group = file.match(/^(.*?)\.[a-z]+$/m);
                bib[databaseName].push(group[1]);
                self._globalCounter++;
                moiConsole.printSimpleCounterUpdateWithMem(self._globalCounter);
            })
            .then(() => {
                console.log("\nFinish " + databaseName)
                resolve();
            })
    });
}

FsConnector.prototype.getDocIdsFromFileNames = async function (parallel, callback) {
    const baseDir = this._filepath;
    let self = this;
    let bib = {};
    return new Promise((resolve, reject) => {
        let processes = [];
        fs.readdirAsync(this._filepath)
            .map(function (name) {
                const stats = fs.statSync(path.join(baseDir, name));

                if ( stats.isDirectory() ){
                    processes.push(self._processFilesInDir(name, path.join(baseDir, name), bib));
                }
            })
            .then(() => {
                Promise.all(processes).then(async () => {
                    console.log("\nFinally we got all files. We can start!");
                    while ( Object.keys(bib).length > 0 ) {
                        let counter = 0;
                        for ( let [db, files] of Object.entries(bib) ) {
                            if ( counter >= parallel ) break;
                            counter++;
                            if ( files.length > 0 ) {
                                await callback({
                                    database: db,
                                    title: files.pop()
                                });
                            } else {
                                delete bib[db];
                                await callback({
                                    shutdown: true,
                                    database: db
                                })
                                console.log("\nFinished requesting files from DB " + db + "; " + Object.keys(bib).length + " databases left.");
                            }
                        }
                    }
                    console.log("Requested all docIDs");
                    resolve();
                });
            });
    });
}

FsConnector.prototype.getMOI = async function (callback) {
    return this._deep(this._filepath, callback);
}

FsConnector.prototype.writeToFile = async function(data) {
    return new Promise((resolve, reject) => {
        const outPath = '/mnt/share/data/arqmath/moiFormulaIDs.txt'
        let writer = fs.createWriteStream(outPath, {
            flags: 'a' // append
        });
        console.log("Start writing to " + outPath);
        let counter = 1;
        for ( let [md5, fids] of Object.entries(data) ) {
            process.stdout.clearLine(0);
            process.stdout.cursorTo(0);
            process.stdout.write("Writing line " + counter++);
            // writer.write(md5 + ' ' + [...fids].join(' ') + '\n');
            writer.write(md5 + ' ' + fids + '\n');
        }
        console.log("\nFinished writing to " + outPath );
        writer.end();
        resolve();
    });
}

module.exports = FsConnector = {
    FSConnector: FsConnector
}