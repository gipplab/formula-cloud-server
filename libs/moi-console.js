const printSimpleCounterUpdate = function(counter) {
    process.stdout.clearLine(0);
    process.stdout.cursorTo(0);
    let msg = "Processed: " + counter;
    process.stdout.write(msg);
}

const printSimpleCounterUpdateWithMem = function(counter) {
    process.stdout.clearLine(0);
    process.stdout.cursorTo(0);
    let used = Math.round((process.memoryUsage().heapUsed / 1024 / 1024)*100)/100;
    let total = Math.round((process.memoryUsage().heapTotal / 1024 / 1024)*100)/100;
    let msg = "Processed: " + counter + " / " +
        "Total memory in use: " + used.toFixed(2) + "/" + total.toFixed(2) + " MB";
    process.stdout.write(msg);
}

const printUpdate = function(
    processedDocuments, childProcStats, queue, runningInstances, waitingForClientsLength
) {
    process.stdout.clearLine(0);
    process.stdout.cursorTo(0);
    let used = Math.round((process.memoryUsage().heapUsed / 1024 / 1024)*100)/100;
    let total = Math.round((process.memoryUsage().heapTotal / 1024 / 1024)*100)/100;
    used += childProcStats.used;
    total += childProcStats.total;
    let strMsg = "Requested Docs from BaseX: " + (processedDocuments.toLocaleString()) +
        " / jobs on hold: " + (queue.pending-1) +
        " / queue size: " + queue.size +
        " / Total memory in use: " + used.toFixed(2) + "/" + total.toFixed(2) + " MB" +
        " / Running Clients: " + runningInstances +
        " / Currently " + waitingForClientsLength + " processes wait for clients.";
    process.stdout.write(strMsg);
}

module.exports = {
    printUpdate: printUpdate,
    printSimpleCounterUpdate: printSimpleCounterUpdate,
    printSimpleCounterUpdateWithMem: printSimpleCounterUpdateWithMem
}