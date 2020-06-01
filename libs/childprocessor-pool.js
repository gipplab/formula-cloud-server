const cp = require('child_process');
const path = require('path');

let allProcesses = [];
let childprocessorPool = [];
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

let handleMessage = function(childProcess, resolve, msg) {
    if ( msg.memoryUsed ) {
        usedMemory[childProcess.pid] = Number(msg.memoryUsed);
        totalHeapMemory[childProcess.pid] = Number(msg.memoryTotal);
    }
    if ( msg.status.startsWith('[DONE]') ){
        // console.log("Process "+childProcess.pid+" finished work and goes back to threadpool.");
        if ( successfullCallback ) successfullCallback(msg);
        childprocessorPool.push(childProcess);
        resolve(); // trigger promise object
    } else if ( msg.status.startsWith('[ERROR]')){
        // console.error("Process "+childProcess.pid+" reported an error: " + msg.status);
        console.error("["+childProcess.pid+'] Error due processing: ' + msg.error);
        childprocessorPool.push(childProcess);
        resolve(); // trigger promise object
    }
};

// functions
let getProcess = function (resolve, processFile) {
    if (childprocessorPool.length) {
        // reuse the child processes (pool size is defined by queue concurrency)
        const childProcess = childprocessorPool.pop();
        // update listeners
        // console.log("Reactivate idle child process "+childProcess.pid);
        childProcess.removeAllListeners('message');
        childProcess.on('message', (msg) => {
            handleMessage(childProcess, resolve, msg);
        });
        return childProcess;
    } else {
        // let portNumber = 1984 + basexCounter;
        // if ( basex ) basexCounter++;
        // const childProcess = cp.fork(path.join(__dirname, processFile), [portNumber.toString()]);
        const childProcess = cp.fork(path.join(__dirname, processFile));
        console.log('Initialize new child process '+childProcess.pid);
        allProcesses.push(childProcess);
        // configure event handler: listening on messages from child process
        childProcess.on('message', (msg) => {
            handleMessage(childProcess, resolve, msg);
        });
        return childProcess;
    }
};

let terminateChildProcesses = () => {
    console.log("");
    console.log("Request shutdown workers.");
    allProcesses.forEach(p => p.disconnect());
    console.log("Workers are down.");
};

module.exports = {
    getProcess: getProcess,
    terminate: terminateChildProcesses,
    setCallbackOnSuccess: setCallbackOnSuccess,
    getMemoryUsage: getMemoryUsage
}

