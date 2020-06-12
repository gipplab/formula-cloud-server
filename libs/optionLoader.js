
function loadOptions (args) {
    let options = {
        'index': 'arqmath',
        'parallel': 1,
        'minTF': 1,
        'minDF': 1,
        'minC': 1,
        'skipSetup': false,
        'in': '/mnt/share/data/arqmath/posts/data.csv',
        'exact': false,
        'searchQuery': "",
        'skipLines': 0,
        'maxClientsPerServer': 1,
        'maxParallelServers': 4,
        'xQueryScript': 'libs/xquery/extractor.xq'
    };

    for (let j = 2; j < args.length; j++) {
        if (args[j] === "-index") {
            j++;
            options["index"] = args[j];
        } else if (args[j] === "-mintf"){
            j++;
            options["minTF"] = args[j];
        } else if (args[j] === "-mindf"){
            j++;
            options["minDF"] = args[j];
        } else if (args[j] === "-minC"){
            j++;
            options["minC"] = args[j];
        } else if (args[j] === "-parallel"){
            j++;
            options["parallel"] = Number(args[j]);
        } else if (args[j] === "-skipSetup"){
            options["skipSetup"] = true;
        } else if (args[j] === "-in"){
            j++;
            options["in"] = args[j];
        } else if (args[j] === "-exact"){
            options["exact"] = true;
        } else if (args[j] === "-skipLines"){
            j++;
            options["skipLines"] = Number(args[j]);
        } else if (args[j] === "-maxClientsPerServer"){
            j++;
            options["maxClientsPerServer"] = Number(args[j]);
        } else if (args[j] === "-maxParallelServers"){
            j++;
            options["maxParallelServers"] = Number(args[j]);
        } else if (args[j] === "-script"){
            j++;
            options["xQueryScript"] = args[j];
        } else {
            options["searchQuery"] += " " + args[j];
        }
    }

    return options;
}

module.exports = {
    loadOptions: loadOptions
}

