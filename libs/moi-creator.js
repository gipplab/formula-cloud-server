const optionLoader = require('./optionLoader');
const fsconnection = require('./fs-connector');
const PQueue = require('p-queue');
const childProcs = require('./childprocessor-pool');
const moiConsole = require('./moi-console');

const options = optionLoader.loadOptions(process.argv);
const queue = new PQueue({
    concurrency: options.parallel
});

const fileLoader = new fsconnection.FSConnector(options.in, options.skipLines);

let processedDocuments = options.skipLines;

const bulkSize = 5_000;
let mathElementsCache = [];

console.log("Parallel: " + options.parallel);

/**
 *
 * @param mathElement {MathElement}
 * @returns {Promise<>}
 */
let handleMathElement = async function ( mathElement ) {
    mathElementsCache.push(mathElement);

    // if cache is not full, just continue
    if ( mathElementsCache.length < bulkSize ) {
        // moiConsole.printUpdate(
        //     processedDocuments,
        //     childProcs.getMemoryUsage(),
        //     queue
        // );
        return Promise.resolve();
    }

    // if the cache is full, remove the bulk and put it to elasticsearch
    const pushElements = mathElementsCache.splice(0, bulkSize);
    return flushElements(pushElements);
}

let flushElements = async function (elementsArray) {
    return queue.add(() => {
        return new Promise((resolve) => {
            let childProcess = childProcs.getProcess(resolve, 'index-worker-moi.js');
            let message = {
                body: elementsArray,
                index: options.index
            };
            childProcess.send(message);
        }).then(() => {
            processedDocuments += elementsArray.length;
            moiConsole.printUpdate(
                processedDocuments,
                childProcs.getMemoryUsage(),
                queue
            );
        });
    });
}

fileLoader.getMOI(handleMathElement)
    .then(() => {
        console.log("Requested all IDs from index.");
        flushElements(mathElementsCache).then(() => {
            queue.onIdle().then(async () => {
                childProcs.terminate();
                console.log("Done, flushed all remaining updates to ES.");
            }).catch((err) => {
                console.error("An error occurred while waiting on the queue " + err);
                console.error(err);
                childProcs.terminate();
            })
        });
    })
    .catch((err) => {
        console.error("Unable to load and store data: " + err);
        console.error(err);
        childProcs.terminate();
    });


