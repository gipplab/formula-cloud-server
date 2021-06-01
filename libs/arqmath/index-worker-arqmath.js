// ES node
const { Client } = require('@elastic/elasticsearch');

// file system manipulations
var BB = require('bluebird');
var fs = BB.promisifyAll(require('fs'));

var client = new Client({
    node: 'http://localhost:9200',
    log: 'info'
});

let cache = [];
const bulkSize = 5_000;

const log = function(msg) {
    // console.log("[pid:"+process.pid+"] " + msg);
};

var indexingDocument = async function (docID, internalID, content, esindex) {
    cache.push({
        "title": docID,
        "internalID": internalID,
        "content": content,
        "url": "https://math.stackexchange.com/questions/"+docID
    });

    if ( cache.length < bulkSize ) {
        process.send({status: "[DONE]"});
        return Promise.resolve();
    }

    const sendCache = cache.splice(0, bulkSize);
    return indexBulk(sendCache, esindex);

    // client.create({
    //     index: esindex,
    //     id: docID,
    //     body: {
    //         "title": docID,
    //         "internalID": internalID,
    //         "content": content,
    //         "url": "https://math.stackexchange.com/questions/"+docID
    //     }
    // }, (err, result) => {
    //     if (err) {
    //         err.content = "...";
    //         log("something went wrong: " + err);
    //         process.send({status: "[ERROR]"});
    //     } else {
    //         log("successfully indexed " + docID);
    //         process.send({status: "[DONE]"});
    //     }
    // });
};

let indexBulk = async function (dataArr, esindex) {
    return client.helpers.bulk({
        datasource: dataArr,
        concurrency: 8,
        onDocument (doc) {
            return {
                index: {
                    _index: esindex,
                    _id: doc.title
                }
            }
        }
    }).then(() => {
        log("successfully indexed bulk.");
        process.send({status: "[DONE]"});
    }).catch((err) => {
        log("something went wrong: " + err);
        process.send({status: "[ERROR]", error: err});
    });
}

process.on('message', (msg) => {
    process.send({status: "[RUN]"});
    if ( msg.flush ) {
        indexBulk(cache, msg.index);
    } else {
        log("processing file " + msg.fileId);
        // var filteredContent = msg.content.replace(/<math.*?>.*?<\/math>|\$+.*?\$+/g, '');
        indexingDocument(msg.fileId, msg.internalID, msg.content, msg.index);
    }
});
