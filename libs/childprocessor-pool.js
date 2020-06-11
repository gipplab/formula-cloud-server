const cp = require('child_process');
const path = require('path');

let childProcessPool = {};
let childProcessPortPool = {};
let childProcessFreedPortsPool = [];

let requestedShutdownMemory = [];

let generalProcessQueue = [];
let allGeneralProcesses = [];

let usedMemory = {};
let totalHeapMemory = {};

let successfullCallback = undefined;

let setCallbackOnSuccess = function (callback) {
    successfullCallback = callback;
}

let getMemoryUsage = function () {
    let usedMem = 0;
    let total = 0;
    for ( let [key, value] of Object.entries(usedMemory) ) {
        usedMem += value;
    }
    for ( let [key, value] of Object.entries(totalHeapMemory) ) {
        total += value;
    }
    return {
        used: Math.round((usedMem / 1024 / 1024)*100)/100,
        total: Math.round((total / 1024 / 1024)*100)/100
    };
}

let checkIfShutdownIsRequested = function(childProcess, callback, database) {
    if ( requestedShutdownMemory.includes(database) ) {
        childProcess.removeAllListeners('message');
        childProcess.on('message', (msg) => {
            handleMessage(childProcess, callback, database, msg);
        });
        childProcess.send({shutdown: true});
    } else {
        if ( childProcessPool[database] )
            childProcessPool[database].push(childProcess);
        callback(); // trigger promise object
    }
}

let handleMessage = function(childProcess, resolve, database, msg) {
    if ( msg.memoryUsed ) {
        usedMemory[childProcess.pid] = Number(msg.memoryUsed);
        totalHeapMemory[childProcess.pid] = Number(msg.memoryTotal);
    }

    if ( msg.status.startsWith('[SHUTDOWN]') ) {
        delete usedMemory[childProcess.pid];
        delete totalHeapMemory[childProcess.pid];
        childProcess.disconnect();
        let freePort = childProcessPortPool[database];
        childProcessFreedPortsPool.push(freePort);
        resolve({
            status: "[SHUTDOWN COMPLETE]"
        }); // done
    } else if ( msg.status.startsWith('[DONE INIT]') ) {
        // after initialization, do not call the successful callback
        console.log("["+childProcess.pid+"] Initialized BaseX Client")
    } else if ( msg.status.startsWith('[DONE]') ){
        // console.log("Process "+childProcess.pid+" finished work and goes back to threadpool.");
        if ( successfullCallback ) successfullCallback(msg);
        checkIfShutdownIsRequested(childProcess, resolve, database);
    } else if ( msg.status.startsWith('[ERROR]')){
        // console.error("Process "+childProcess.pid+" reported an error: " + msg.status);
        console.error("["+childProcess.pid+'] Error due processing: ' + msg.error);
        checkIfShutdownIsRequested(childProcess, resolve, database);
    }
};

let handleGeneralProcessMessage = function(childProcess, resolve, msg) {
    if ( msg.memoryUsed ) {
        usedMemory[childProcess.pid] = Number(msg.memoryUsed);
        totalHeapMemory[childProcess.pid] = Number(msg.memoryTotal);
    }

    if ( msg.status.startsWith('[RUN]') ) {
        // it's running now, take some time to finish.
        return;
    }

    if ( msg.status.startsWith('[DONE]') ){
        // console.log("Process "+childProcess.pid+" finished work and goes back to threadpool.");
        if ( successfullCallback ) successfullCallback(msg);
    } else if ( msg.status.startsWith('[ERROR]')){
        // console.error("Process "+childProcess.pid+" reported an error: " + msg.status);
        console.error("["+childProcess.pid+'] Error due processing: ' + msg.error);
    } else {
        console.error("Received an unsupported message from a childprocess. Donno what to do.")
        console.error(msg);
    }

    generalProcessQueue.push(childProcess);
    resolve();
};

/**
 *
 * @param resolve
 * @param processFile
 * @returns {ChildProcess|*}
 */
let getProcess = function (resolve, processFile){
    let childProcess = undefined;
    if ( generalProcessQueue.length ) {
        // reuse the child processes (pool size is defined by queue concurrency)
        childProcess = generalProcessQueue.pop();
        childProcess.removeAllListeners('message');
    } else {
        childProcess = cp.fork(path.join(__dirname, processFile));
        console.log('Initialize new child process '+childProcess.pid);
        allGeneralProcesses.push(childProcess);
    }
    // configure event handler: listening on messages from child process
    childProcess.on('message', (msg) => {
        handleGeneralProcessMessage(childProcess, resolve, msg);
    });
    return childProcess;
};

/**
 *
 * @param resolve
 * @param database {String}
 * @param xQueryScript {String} path to script
 */
let getBaseXProcess = function(resolve, database, xQueryScript) {
    if ( childProcessPool[database] && childProcessPool[database].length > 0 ) {
        // client process exists
        const childProcess = childProcessPool[database].pop();
        // update listener
        childProcess.removeAllListeners('message');
        childProcess.on('message', (msg) => {
            handleMessage(childProcess, resolve, database, msg);
        });
        return childProcess;
    } else {
        if ( !childProcessPortPool[database] ) {
            if ( childProcessFreedPortsPool.length > 0 ) {
                childProcessPortPool[database] = childProcessFreedPortsPool.pop();
            } else {
                childProcessPortPool[database] = 1984 + Object.keys(childProcessPool).length;
            }
        }
        childProcessPool[database] = [];
        const childProcess = cp.fork(
            path.join(__dirname, 'basex-connector.js'),
            [
                childProcessPortPool[database],
                xQueryScript,
                database
            ]
        );
        childProcess.on('message', (msg) => {
            handleMessage(childProcess, resolve, database, msg);
        });
        return childProcess;
    }
}

let shutdownBaseXDBClient = function(resolve, database) {
    let promisMem = [];
    requestedShutdownMemory.push(database);
    while ( childProcessPool[database].length > 0 ) {
        const childProcess = childProcessPool[database].pop();
        promisMem.push(new Promise((innerResolve, innerReject) => {
            childProcess.removeAllListeners('message');
            childProcess.on('message', (msg) => {
                handleMessage(childProcess, innerResolve, database, msg);
            });
            childProcess.send({shutdown: true});
        }));
    }
    Promise.all(promisMem).then(() => {
        resolve({
            status: "[SHUTDOWN COMPLETE]"
        });
    })
}

let terminateChildProcesses = () => {
    console.log("");
    console.log("Request shutdown workers.");
    for ( let [database, procs] of Object.entries(childProcessPool) ) {
        while ( procs && procs.length > 0 ) {
            const childProcess = procs.pop();
            childProcess.send({
                shutdown: true
            });
        }
        console.log("Requested shutdown for all workers of DB " + database);
    }

    allGeneralProcesses.forEach(childProc => {
        childProc.disconnect();
    });

    console.log("Reqeuested shutdown for all workers.");
};

module.exports = {
    getProcess: getProcess,
    getBaseXProcess: getBaseXProcess,
    shutdownBaseXDBClient: shutdownBaseXDBClient,
    terminate: terminateChildProcesses,
    setCallbackOnSuccess: setCallbackOnSuccess,
    getMemoryUsage: getMemoryUsage
}

