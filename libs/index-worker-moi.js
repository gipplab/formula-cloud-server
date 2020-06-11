// ES node
const { Client } = require('@elastic/elasticsearch');
const crypto = require('crypto');
const md5Hash = crypto.createHash('md5');

// file system manipulations
// var BB = require('bluebird');
// var fs = BB.promisifyAll(require('fs'));

const client = new Client({
    node: 'http://localhost:9200',
    log: 'debug'
});

const log = function(msg) {
    // console.log("[pid:"+process.pid+"] " + msg);
};

let indexingDocument = function (dataArray, esindex) {
    client.helpers.bulk({
        datasource: dataArray,
        concurrency: 8,
        onDocument (doc) {
            return {
                index: {
                    _index: esindex,
                    _id: md5Hash.copy().update(doc.moi).digest('base64')
                }
            }
        }
    }).then(() => {
        log("successfully indexed bulk.");
        process.send({status: "[DONE]"});
    }).catch((err) => {
        // err.content = "...";
        log("something went wrong: " + err);
        process.send({status: "[ERROR]", error: err});
    });
};

process.on('message', (msg) => {
    process.send({status: "[RUN]"});
    // log("processing " + msg);
    // var filteredContent = msg.content.replace(/<math.*?>.*?<\/math>|\$+.*?\$+/g, '');
    indexingDocument(msg.body, msg.index);
});
