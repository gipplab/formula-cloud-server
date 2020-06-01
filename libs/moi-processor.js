var concurrencyLevel = 8;
var data = '/home/andre/data/arqmath/moi';
var esindex = 'arqmath-moi';
var skipSetup = false;
//var max = 1000;

const bulkSize = 10_000;

for (let j = 0; j < process.argv.length; j++) {
    if (process.argv[j] === "-parallel"){
        j++;
        concurrencyLevel = Number(process.argv[j]);
    } else if (process.argv[j] === "-in") {
        j++;
        data = process.argv[j];
    } else if (process.argv[j] === "-skipSetup") {
        skipSetup = true;
    } else if (process.argv[j] === "-max") {
        j++;
        max = Number(process.argv[j]);
    } else if (process.argv[j] === "-index") {
        j++;
        esindex = process.argv[j];
    }
}

// ES node
const { Client } = require('@elastic/elasticsearch');

// file system manipulations
var BB = require('bluebird');
var fs = BB.promisifyAll(require('fs'));
var LineByLineReader = require('line-by-line');
const path = require('path');

// parallel working
var PQueue = require('p-queue');

const cp = require('child_process');
const queue = new PQueue({
    concurrency: concurrencyLevel
});

var allProcesses = [];
var processPool = [];

var handleMessage = function(childProcess, resolve, msg){
    if ( msg.status.startsWith('[DONE]') ){
        // console.log("Process "+childProcess.pid+" finished work and goes back to threadpool.");
        processPool.push(childProcess);
        resolve(); // trigger promise object
    } else if ( msg.status.startsWith('[ERROR]')){
        // console.error("Process "+childProcess.pid+" reported an error: " + msg.status);
        processPool.push(childProcess);
        resolve(); // trigger promise object
    }
};

// functions
const getProcess = function (resolve) {
    if (processPool.length) {
        // reuse the child processes (pool size is defined by queue concurrency)
        const childProcess = processPool.pop();
        // update listeners
        // console.log("Reactivate idle child process "+childProcess.pid);
        childProcess.removeAllListeners('message');
        childProcess.on('message', (msg) => {
            handleMessage(childProcess, resolve, msg);
        });
        return childProcess;
    } else {
        // not enough child processes, start a new one!
        const childProcess = cp.fork(path.join(__dirname, 'index-worker-moi.js'));
        console.log('Initialize new child process '+childProcess.pid);
        allProcesses.push(childProcess);
        // configure event handler: listening on messages from child process
        childProcess.on('message', (msg) => {
            handleMessage(childProcess, resolve, msg);
        });
        return childProcess;
    }
};

const terminateChildProcesses = () => {
    console.log("");
    console.log("Request shutdown workers.");
    allProcesses.forEach(p => p.disconnect());
    console.log("Workers are down.");
};

console.log("Establish connection to elasticsearch.");

var client = new Client({
    node: 'http://localhost:9200',
    log: 'info'
});

console.log("Done. Connected with ES.");
let counter = 0;

let addElementToProcessQueue = function(elements) {
    queue.add(() => {
        // console.log("Adding " + id + " to queue...");
        return new Promise((resolve) => {
            const childProcess = getProcess(resolve);
            // send message to process
            //console.log('Trigger child process '+childProcess.pid);
            childProcess.send({
                index: esindex,
                body: elements
            });
        }).then(() => {
            // console.log("Files indexed: " + (counter++) + " / jobs on hold: " + (queue.pending-1) + "\r");
            process.stdout.clearLine(0);
            process.stdout.cursorTo(0);
            counter += elements.length;
            let strMsg = "Indexed: " + (counter) + " / jobs on hold: " + (queue.pending-1);
            process.stdout.write(strMsg);
            // console.log(strMsg);
        });
    });
}

function lines(file) {
    return new Promise(function(resolve, reject) {
        console.log("Start file: " + file);
        console.log("");
        let lineMemory = [];
        let lineReader = new LineByLineReader(file, {encoding: 'utf8', skipEmptyLines: true});

        lineReader.on('error', function(err) {
            console.log("Error on reading lines: " + err);
            reject(new Promise.CancellationError());
        });

        lineReader.on('line', function(line) {
            if ( lineMemory.length >= bulkSize ) {
                addElementToProcessQueue(lineMemory);
                lineMemory = [];
            }

            let groups = line.match(/^"(.*)";(\d+);(\d+);(\d+)$/m);
            let moi = groups[1];
            let depth = groups[2];
            let tf = groups[3];
            let df = groups[4];
            lineMemory.push({
                moi: moi,
                complexity: depth,
                tf: tf,
                df: df
            });
        });

        lineReader.on('end', function() {
            if ( lineMemory.length > 0 ) {
                // need to push the remaining elements
                addElementToProcessQueue(lineMemory);
                lineMemory = [];
            }

            console.log("");
            console.log('Done reading all lines from: ' + file);
            // console.log("Added all elements to queue. Wait until all process finished.");
            console.log("");

            resolve([counter]);
        });
    });
}

function deep(dir) {
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
            if (res == null) return false;
            return true;
        });
}

startProcessing = function() {
    deep(data)
        .map(lines)
        .then(() => {
            console.log("All files indexed.");
            queue.onIdle().then(()=>{
                terminateChildProcesses();
            });
        });
};


// setup index
if ( !skipSetup ){
    console.log("Setup analyzer for ES.");
    var analyzerSetup = JSON.parse(fs.readFileSync("analyzers/moi-analyzer.json"));
    console.log("Parsed analyzer setup. Create index.");
    client.indices.create({
        index: esindex,
        body: analyzerSetup
    }, (err,msg) => {
        if (err){
            if ( err.body.error.type === "resource_already_exists_exception" ){
                console.error("You tried to setup a new index but the index already exists. Delete the old index first or use another index instead. Deleting it by:");
                console.error("curl -X DELETE \"localhost:9200/"+esindex+"\"");
            } else {
                console.error("Something went wrong during setup the analyzer. Stop here");
                console.log(err.body);
            }
            process.exit();
        } else {
            console.log("Done setup analyzer.");
            console.log(msg.body);
            startProcessing(); // trigger once the setup is ready
        }
    });
} else {
    startProcessing(); // trigger the process directly
}
