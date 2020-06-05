const optionLoader = require('./optionLoader');
// const baseXConnector = require('./basex-connector');
const ESConnectorFactory = require('./es-connector');
const fsconnection = require('./fsConnector');
const PQueue = require('p-queue');
const childProcs = require('./childprocessor-pool');
// const readline = require('readline');

const options = optionLoader.loadOptions(process.argv);
const es = new ESConnectorFactory.ESConnector(options);
es.setIndex(options.index);

// const basex = new baseXConnector.BaseXConnector();

const basexQueue = new PQueue({
    concurrency: options.parallel
});

const fileLoader = new fsconnection.FSConnector(options.in, options.skipLines);

let updateESIndex = async function ( data ) {
    // es.updateIndex(data.mois, data.docID);
}

childProcs.setCallbackOnSuccess(updateESIndex);

let processedDocuments = options.skipLines;
let handleIDChunk = async function ( doc ) {
    let queueAddedPromise = basexQueue.add(() => {
        return new Promise((resolve) => {
            let childProcess = childProcs.getProcess(resolve, 'basex-connector.js');
            let message = {
                docID: doc.title
            };
            if ( doc.database ) message['database'] = doc.database;

            childProcess.send(message);
        }).then(() => {
            process.stdout.clearLine(0);
            process.stdout.cursorTo(0);
            processedDocuments++;
            let used = Math.round((process.memoryUsage().heapUsed / 1024 / 1024)*100)/100;
            let total = Math.round((process.memoryUsage().heapTotal / 1024 / 1024)*100)/100;
            let childProcStats = childProcs.getMemoryUsage();
            used += childProcStats.used;
            total += childProcStats.total;
            let strMsg = "Requested Docs from BaseX: " + (processedDocuments) +
                " / jobs on hold: " + (basexQueue.pending-1) +
                " / queue size: " + basexQueue.size +
                " / Total memory in use: " + used.toFixed(2) + "/" + total.toFixed(2) + " MB";
            process.stdout.write(strMsg);
        });
    });
    if ( basexQueue.size < 10_000 ) return Promise.resolve();
    else return queueAddedPromise;
}

// es.loadDocumentIDs(handleIDChunk)
fileLoader.getDocIds(handleIDChunk)
    .then(() => {
        console.log("Requested all IDs from index.");
        basexQueue.onIdle().then(async () => {
            childProcs.terminate();
            await es.flushUpdates();
            console.log("Done, flushed all remaining updates to ES.");
            es.close();
        });
    })
    .catch((err) => {
        console.error("Unable to load and store data: " + err);
        console.error(err);
        childProcs.terminate();
        es.close();
    });


