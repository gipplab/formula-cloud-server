var concurrencyLevel = 8;
var data = '/home/andre/data/arqmath/posts/testData.csv';
var esindex = 'arqmath';
var skipSetup = false;
//var max = 1000;

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
        const childProcess = cp.fork(path.join(__dirname, 'index-worker-arqmath.js'));
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
var counter = 0;

function lines(file) {
    let lineReader = new LineByLineReader(file, {encoding: 'utf8', skipEmptyLines: true});

    lineReader.on('error', function(err) {
        console.log("Error on reading lines: " + err);
    });

    lineReader.on('line', function(line) {
        let groups = line.match(/^(\d+),"?(.*?)"?$/m);
        let id = groups[1];
        let content = groups[2];

        queue.add(
            () => {
                // console.log("Adding " + id + " to queue...");
                const processPromise = new Promise((resolve) => {
                    const childProcess = getProcess(resolve);
                    // send message to process
                    //console.log('Trigger child process '+childProcess.pid);
                    childProcess.send({fileId: id, content: content, index: esindex});
                });
                return processPromise.then(() => {
                    // console.log("Files indexed: " + (counter++) + " / jobs on hold: " + (queue.pending-1) + "\r");
                    process.stdout.clearLine(0);
                    process.stdout.cursorTo(0);
                    let strMsg = "Files indexed: " + (counter++) + " / jobs on hold: " + (queue.pending-1);
                    process.stdout.write(strMsg);
                });
            }
        );
    });

    lineReader.on('end', function() {
        console.log("");
        console.log('Done! Send all lines to ES. Total lines: ' + counter);
        console.log("Added all elements to queue. Wait until all process finished.");
        console.log("");
        queue.onIdle().then(()=>{
            terminateChildProcesses();
        });
    });
}

startProcessing = function() {
    lines(data);
};


// setup index
if ( !skipSetup ){
    console.log("Setup analyzer for ES.");
    var analyzerSetup = JSON.parse(fs.readFileSync("arqmath-analyzer.json"));
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
