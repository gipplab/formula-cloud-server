const myconsole = require('./moi-console');
let arr = [];
for ( let i = 0; i < 100_000_000; i++ ) {
    arr.push(i);
}

while( arr.length ) {
    arr.splice(0, 10);
    myconsole.printSimpleCounterUpdateWithMem(arr.length);
}