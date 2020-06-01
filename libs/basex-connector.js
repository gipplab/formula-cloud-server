const fs = require('fs');
const basex = require('basex');

let MathElement = function( expression, complexity, tf ) {
    this.expression = expression;
    this.complexity = complexity;
    this.termFrequency = tf;
}

let BaseXConnector = function( port, databases, xQueryPath ) {
    this._options = {
        port: port || 1984,
        databases: databases || ["out"],
        xQuery: fs.readFileSync(
            xQueryPath || 'libs/basex/extractor.xq',
            'utf8'
        )
    };
    this._sessions = {};
    this._init();
}

BaseXConnector.prototype._init = function() {
    let sessions = this._sessions;
    this._options.databases.forEach((value, index) => {
        let takenPort = this._options.port + index;

        console.log("Init BaseX DB " + value + " on port " + takenPort);
        sessions[value] = new basex.Session('127.0.0.1', takenPort, 'admin', 'admin');
        sessions[value].execute('OPEN ' + value, (err, resp) => {
            if ( err ) throw err;
            console.log("Successfully initiated BaseX DB: " + resp);
        });
    });
}

BaseXConnector.prototype.close = function() {
    Object.keys(this._sessions).forEach((key, index) => {
        this._sessions[key].close();
    });
    console.log("Closed all BaseX connections.");
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
        let client = this._sessions[db];
        let query = client.query(this._options.xQuery);

        query.bind('$docid', id, '', (err, result) => {
            if ( err ) {
                reject('Unable to bind docID to query. Reason: ' + err);
                return null;
            }

            // we can ignore the result from the bind operation, just move on here
            query.results((err, data) => {
                if ( err ) {
                    reject(err);
                } else {
                    let mappedValues = data.result.map(line => {
                        let groups = line.match(/<element.*freq="(\d+)".*depth="(\d+)".*>(.*)<\/element>/m);
                        return new MathElement(groups[3], groups[2], groups[1]);
                    });
                    resolve(mappedValues);
                }
            });
        });
    });
}

// const basexInstance = new BaseXConnector(Number(process.argv[2]), null, null);
const basexInstance = new BaseXConnector();

process.on('message', (message) => {
    process.send({
        status: '[RUN]',
        memoryUsed: process.memoryUsage().heapUsed,
        memoryTotal: process.memoryUsage().heapTotal
    });

    basexInstance.loadMOIs(message.docID, message.database)
        .then((values, err) => {
            if (err) {
                process.send({
                    status: '[ERROR]',
                    error: err,
                });
            } else {
                process.send({
                    status: '[DONE]',
                    mois: values,
                    docID: message.docID
                });
            }
        });
});

module.exports = BaseXConnector = {
    BaseXConnector: BaseXConnector,
    MathElement: MathElement
}
