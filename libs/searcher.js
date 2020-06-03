// ES node
const {Client} = require('@elastic/elasticsearch');
const crypto = require('crypto');
const hashFunc = crypto.createHash('md5')

var esindex = 'arqmath';
var searchQuery = "";
var minC = 1;
var minTF = 1;
var minDF = 1;
var exact = false;

for (let j = 2; j < process.argv.length; j++) {
    if (process.argv[j] === "-index") {
        j++;
        esindex = process.argv[j];
    } else if (process.argv[j] === "-mintf"){
        j++;
        minTF = process.argv[j];
    } else if (process.argv[j] === "-mindf"){
        j++;
        minDF = process.argv[j];
    } else if (process.argv[j] === "-minC"){
        j++;
        minC = process.argv[j];
    } else if (process.argv[j] === "-exact"){
        exact = true;
    } else {
        searchQuery += " " + process.argv[j];
    }
}

searchQuery = searchQuery.substr(1, searchQuery.length);

var client = new Client({
    node: 'http://localhost:9200',
    log: 'info'
});

if ( exact ) {
    console.log("Search for exact match of: " + searchQuery);
    let md5Value = hashFunc.copy().update(searchQuery).digest('base64');

    console.log("MD5 value to search for: " + md5Value);
    client.get({
        index: esindex,
        id: md5Value
    }, (err, msg) => {
        if (err) {
            console.error(err);
        } else {
            console.log(JSON.stringify(msg.body, null, 4));
        }
    });
} else {
    console.log("Searching for '" + searchQuery + "'");
    client.search({
        index: esindex,
        pretty: true,
        body: {
            query: {
                bool: {
                    must: [
                        {
                            match: {
                                "content": {
                                    query: searchQuery,
                                    minimum_should_match: "50%"
                                }
                            }
                        },
                        {
                            nested: {
                                path: "moi",
                                query: {
                                    exists: {
                                        field: "moi.moiMD5"
                                    }
                                }
                            }
                        }
                    ],
                    should: {
                        match_phrase: {
                            "content": {
                                query: searchQuery,
                                slop: 10
                            }
                        }
                    }
                }
            },
            _source: ["title", "content", "moi", "arxiv"]
            // suggest: {
            //     "my-suggestions": {
            //         text: searchQuery,
            //         phrase: {field: "moi"}
            //     }
            // }
        }
    }, (err, msg) => {
        if (err) {
            console.error(err);
        } else {
            console.log(JSON.stringify(msg.body, null, 4));
        }
    });
}
