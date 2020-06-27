const optionLoader = require('./optionLoader');
const ESConnectorFactory = require('./es-connector');
const fsconnection = require('./fs-connector');
const PQueue = require('p-queue');
const childProcs = require('./childprocessor-pool');
const moiConsole = require('./moi-console');
// const Cache = require('./cache');
// const baseXConnector = require('./basex-connector');
// const readline = require('readline');
// const cache = new Cache.Cache();

const options = optionLoader.loadOptions(process.argv);
const es = new ESConnectorFactory.ESConnector(options);
es.setIndex(options.index);

// const basex = new baseXConnector.BaseXConnector();

const basexQueue = new PQueue({
    concurrency: options.parallel
});

const fileLoader = new fsconnection.FSConnector(options.in, options.skipLines);

let updateESIndex = async function ( data ) {
// console.log("Retrieved result from docID " + data.docID + " ("+data.database+")");
    if (data.mois.length > 0)
        es.addMOIToTextIndex(data.mois, data.docID);

    // console.log("Update: " + data.docID);
    // if ( data.mois.length === 0 ) {
        // console.log("No Update because NO MOI found in " + data.docID + " in " + data.database);
    // }
    // await es.addFormulaIDsToMOI(data.mois);
    // await cache.addFormulaIDsToMOI(data.mois);
}

childProcs.setCallbackOnSuccess(updateESIndex);
childProcs.setMaxClientsPerServer(options.maxClientsPerServer);

let processedDocuments = options.skipLines;
let handleIDChunk = async function ( doc ) {
    let queueAddedPromise = basexQueue.add(() => {
        return new Promise((resolve) => {
            if ( doc.shutdown ) {
                console.log("\nDatabase " + doc.database + " finished. Requesting shutdown clients.");
                childProcs.shutdownBaseXDBClient(resolve, doc.database);
                return;
            }

            childProcs.getBaseXProcess(resolve, doc.database, options.xQueryScript)
                .then((childProcess) => {
                    console.log("DB: " + doc.database);
                    let message = {
                        docID: doc.title
                    };
                    if ( doc.database ) message['database'] = doc.database;

                    childProcess.send(message);
                });
        }).then((msg) => {
            if ( msg && msg.status === "[SHUTDOWN COMPLETE]" ) {
                // no update required.
            } else {
                processedDocuments++;
                moiConsole.printUpdate(
                    processedDocuments,
                    childProcs.getMemoryUsage(),
                    basexQueue,
                    childProcs.getTotalNumberOfRunningInstances(),
                    childProcs.getTotalNumberOfWaitForClientCalls()
                );
            }
        });
    });

    if ( basexQueue.size < 10_000 )
        return Promise.resolve();
    else return queueAddedPromise;
}

// es.loadDocumentIDs(handleIDChunk)
// fileLoader.getDocIds(handleIDChunk)
fileLoader.getDocIdsFromFileNames(options.maxParallelServers, handleIDChunk)
    .then(() => {
        basexQueue.onIdle().then(async () => {
            await childProcs.terminate();
            // await es.flushMOIToTextMemory();
            await es.close();
            console.log("Done, indexed all MOI, closed all services.");
        })
    });
    // .then(() => {
    //     console.log("Requested all IDs from index.");
    //     basexQueue.onIdle().then(async () => {
    //         childProcs.terminate();
    //         await es.flushUpdates();
    //         console.log("Done, flushed all remaining updates to ES.");
    //         es.close();
    //     });
    // })
    // .catch((err) => {
    //     console.error("Unable to load and store data: " + err);
    //     console.error(err);
    //     childProcs.terminate();
    //     es.close();
    // });


