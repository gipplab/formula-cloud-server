const BB = require('bluebird');
const fs = BB.promisifyAll(require('fs'));
const LineByLineReader = require('line-by-line');
const path = require('path');
const moiConsole = require('./moi-console');

let FsConnector = function (filePath, skipLines) {
    this._filepath = filePath;
    this._skipLines = skipLines;
    this._globalCounter = 0;
    this._taskList = [];
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

/**
 *
 * @param bib {Object}
 * @param callback {Function}
 * @param runningPromises {Array<Promise>}
 * @returns {Generator<*, void, *>}
 * @private
 */
FsConnector.prototype._databaseFileGenerator = function* (bib, callback, runningPromises) {
    for ( let [db, files] of Object.entries(bib) ) {
        let promise = new Promise(async(resolve) => {
            while ( files.length > 0 ) {
                await callback({
                    database: db,
                    title: files.pop()
                });
            }
            // once all files from this DB were called, we are done
            resolve();
        }).then(() => {
            // so this db finished, hence it's quite simple, we call the generator again
            // and we dont care about the return value, we just trigger to add a new DB
            this._databaseFileGenerator(bib, callback, runningPromises).next();
        });
        runningPromises.push(promise);
        yield runningPromises;
    }
}

FsConnector.prototype._taskListTrigger = function * () {
    while ( this._taskList.length > 0 ) {
        let resolve = this._taskList.pop();
        yield resolve();
    }
}

/**
 *
 * @param trigger {Generator}
 * @param callback {Function}
 * @param bib {Object}
 * @private
 */
FsConnector.prototype._generateTaskList = function (trigger, callback, bib) {
    const self = this;
    let promiseList = [];
    for ( let [db, files] of Object.entries(bib) ) {
        let finishedDBResolve = undefined;
        let dbFinishedPromise = new Promise((fdbResolve) => {
            finishedDBResolve = fdbResolve;
        });
        promiseList.push(dbFinishedPromise);
        new Promise((resolve) => {
            self._taskList.push(resolve);
        }).then(async () => {
            // got triggered
            console.log("\nStart processing files from DB " + db);
            while ( files.length > 0 ) {
                await callback({
                    database: db,
                    title: files.pop()
                });
            }
            // done
            await callback({
                shutdown: true,
                database: db
            });
            console.log("\nFinish processing files from DB " + db + ".");
            // now we are done with this database! so it's time to trigger the next, if any
            trigger.next();
            // this db is finished, so trigger the promise that waits for this state
            finishedDBResolve();
        });
        // the values are set inside the scope of the promise..
        // hence we can get ridof the rest
        delete bib[db];
    }
    return promiseList;
}

/**
 *
 * @param bib {Object}
 * @param callback {Function}
 * @param parallel {Number}
 * @private
 */
FsConnector.prototype._startCallbackSequence = function (bib, callback, parallel) {
    const taskTrigger = this._taskListTrigger();
    const promiseList = this._generateTaskList(taskTrigger, callback, bib);
    // kick of as many processes in parallel as we specified
    for ( let i = 0; i < parallel; i++ ) {
        let task = this._taskList.pop();
        task();
    }
    // all triggered, the rest runs automatically
    return promiseList;
}

FsConnector.prototype.getDocIdsFromFileNames = async function (parallel, callback) {
    const baseDir = this._filepath;
    const self = this;
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
                    const tasksPromises = self._startCallbackSequence(bib, callback, parallel);
                    Promise.all(tasksPromises)
                        .then(() => {
                            console.log("Requested all docIDs");
                            resolve();
                        });

                    // while ( Object.keys(bib).length > 0 ) {
                    //     let counter = 0;
                    //     for ( let [db, files] of Object.entries(bib) ) {
                    //         if ( counter >= parallel ) break;
                    //         counter++;
                    //         if ( files.length > 0 ) {
                    //             await callback({
                    //                 database: db,
                    //                 title: files.pop()
                    //             });
                    //         } else {
                    //             delete bib[db];
                    //             await callback({
                    //                 shutdown: true,
                    //                 database: db
                    //             })
                    //             console.log("\nFinished requesting files from DB " + db + "; " + Object.keys(bib).length + " databases left.");
                    //         }
                    //     }
                    // }
                    // console.log("Requested all docIDs");
                    // resolve();
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