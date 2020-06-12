# Formula Cloud Server

```shell script
# Script libs/moi-creator.js loads MOI from text files to index them at ES
#   --max-old-space-size  the heap node can use (in MB)
#   -parallel   how many indexing processes shall run in parallel
#   -in         the directory of MOI files
#   -index      the ES index you want to push the MOIs
# Note that you MUST create the index before you start the process
# The setting for MOIs is in libs/analyzers/moi-analyzer.json
andreg-p@csisv15:~/formula-cloud-server$ node --max-old-space-size=100000 libs/moi-creator.js -parallel 16 -in /home/andreg-p/arxmliv/math-stats/minTF1-ALL/ -index arxiv-moi
```

```shell script
# 1) Make sure you update .basex
# 2) Update ./helper/basexServerStartup.sh line 30 to start the basex servers
# 
andreg-p@csisv15:~/formula-cloud-server$ ./helper/basexServerStartup.sh ../arxmliv/math-basex-arxiv/
andreg-p@csisv15:~/formula-cloud-server$ node --max-old-space-size=100000 libs/moi-updater.js -parallel 25 -in /home/andreg-p/arxmliv/math-basex-arxiv/ -index arxiv-tmp -script libs/xquery/extractorARXIV.xq
```