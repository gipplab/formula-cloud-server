const fs = require('fs');
const basex = require('./basex/index');
const me = require('./mathelement');

let BaseXConnector = function() {
    this.initialized = false;
}

// let BaseXConnector = function( port, databases, xQueryPath ) {
//     this.setup(port, databases, xQueryPath);
// }

BaseXConnector.prototype.setup = function(port, databases, xQueryPath ) {
    this._options = {
        port: port || 1984,
        databases: databases || ["out"],
        xQuery: fs.readFileSync(
            xQueryPath || 'libs/xquery/extractor.xq',
            'utf8'
        )
    };
    this._sessions = {};
    this._queries = {};
    this._init();
}

BaseXConnector.prototype._init = function() {
    let sessions = this._sessions;
    const self = this;
    this._options.databases.forEach((value, index) => {
        let takenPort = this._options.port + index;

        console.log("Init BaseX Client for DB '" + value + "' on port " + takenPort);
        sessions[value] = new basex.Session('127.0.0.1', takenPort, 'admin', 'admin');
        this._queries[value] = sessions[value].query(this._options.xQuery);
        sessions[value].execute('OPEN ' + value, (err, resp) => {
            if ( err ) {
                process.send({
                    status: '[ERROR]',
                    message: "Unable to init basex server: " + err
                })
            }
            console.log("Successfully initiated BaseX client on port " + takenPort + " for DB " + value);

            if ( index === self._options.databases.length-1 ) {
                process.send({
                    status: '[DONE INIT]'
                });
            }
        });
    });
    this.initialized = true;
}

BaseXConnector.prototype.close = function() {
    Object.keys(this._sessions).forEach((key, index) => {
        this._sessions[key].close();
    });
    console.log("Closed all BaseX connections " + this._options.databases);
    this._sessions = {};
}

/**
 *
 * @param id {String}
 * @param db {String}
 * @returns {Promise|Array}
 */
BaseXConnector.prototype.loadMOIs = async function(id, db = this._options.databases[0] ) {
    return new Promise((resolve, reject) => {
        if ( Object.keys(this._sessions).length === 0 ) {
            reject("No BaseX sessions initialized but tried to retrieve MOI.");
            return;
        }

        // console.log("Request MOIs from BaseX for " + id);
        // let client = this._sessions[db];
        // let query = client.query(this._options.xQuery);
        let query = this._queries[db];

        query.bind('$docid', id, '', (err, result) => {
            if ( err ) {
                console.error(err);
                reject('Unable to bind docID to query. Reason: ' + err);
                return null;
            }

            // we can ignore the result from the bind operation, just move on here
            query.results((err, data) => {
                if ( err ) {
                    reject(err);
                } else {
                    let mappedValues = data.result.map(line => {
                        let groups =
                            line.match(/<element.*?freq="(\d+)" depth="(\d+)" fIDs="(.*?)" docIDs="(.*?)">(.*)<\/element>/m);
                        return new me.MathElement(groups[5], groups[2], groups[1], groups[3], groups[4]);
                    });
                    resolve(mappedValues);
                }
            });
        });
    });
}

// const basexInstance = new BaseXConnector(1984+Number(process.argv[2]), null, null);
let basexInstance = new BaseXConnector();
basexInstance.setup(
    Number(process.argv[2]),
    process.argv.slice(4, process.argv.length),
    process.argv[3]
);

process.on('message', async (message) => {
    if ( message.shutdown ) {
        console.log("["+process.pid+"] received shutdown request. Shutdown basex connections.");
        basexInstance.close();
        process.send({
            status: '[SHUTDOWN]'
        });
        return;
    }

    process.send({
        status: '[RUN]',
        memoryUsed: process.memoryUsage().heapUsed,
        memoryTotal: process.memoryUsage().heapTotal
    });

    basexInstance.loadMOIs(message.docID, message.database)
        .then(async (values, err) => {
            if (err) {
                process.send({
                    status: '[ERROR]',
                    error: err,
                });
            } else {
                // let's test longer waiting times, just for debugging
                // let's fake up to 2 seconds waiting time for each BaseX request
                // for large DBs it might increase up to 10 seconds... (eg on arxiv)
                let waitTime = Math.floor(Math.random()*1000);
                await new Promise(r => setTimeout(r, waitTime));

                process.send({
                    status: '[DONE]',
                    mois: values,
                    docID: message.docID,
                    database: message.database
                });
            }
        }).catch(err => {
            console.error(err);
            process.send({
                status: '[ERROR]',
                error: err,
            });
        })
});

module.exports = BaseXConnector = {
    BaseXConnector: BaseXConnector
}
