const myconsole = require('./moi-console');

let group = "astro-ph9912451.xml".match(/^(.*?)\.[a-z]+$/m);
console.log(group[1]);

let bib = {
    "alpha": ["a", "b", "c"],
    "numbs": ["1", "2", "3"],
    "greek": ["alpha", "beta", "gamma"],
    "abbr": ["agp", "gdp", "gte"]
};

const tasksList = [];
const promiseList = [];

let generator = function * () {
    while ( tasksList.length > 0 ) {
        let resolve = tasksList.pop();
        yield resolve();
    }
}

/**
 *
 * @param iterator {Generator}
 */
let fillTaskList = function (iterator) {
    for ( let [e, v] of Object.entries(bib) ) {
        let res = undefined;
        let p = new Promise((innerRes) => {
            res = innerRes;
        });
        promiseList.push(p);
        new Promise((resolve) => {
            tasksList.push(resolve);
        }).then(async () => {
                // gets triggered
                console.log("Lets start " + e);
                while ( v.length > 0 ) {
                    console.log(v.pop());
                    const waitTime = Math.floor(Math.random()*5000);
                    console.log("Wait for " + (waitTime/1000) + "s");
                    await new Promise(r => setTimeout(r, waitTime));
                }
                iterator.next();
                res();
            });
        delete bib[e];
    }
}

const gen = generator();
console.log(bib);
fillTaskList(gen);
console.log(bib);

console.log("Let's start");
// console.log(tasksList);
let resolve = tasksList.pop();
let resolve2 = tasksList.pop();
resolve();
resolve2();

Promise.all(promiseList).then(() => {
    console.log("DONE! All done... I can't believe it, took ages.");
})
