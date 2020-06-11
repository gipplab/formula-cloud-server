#!/bin/bash

function on_exit () {
  echo "" # empty line
  echo "Received shutdown signal. Stop all BaseX servers."
  portCounter=$port
  for pid in "${pids_memory[@]}"
  do
    echo "[$pid] Request BaseX server on port $portCounter to shutdown"
    kill "$pid"
    echo "[$pid] Waiting for server (port $portCounter) to exit"
    wait "$pid"
    echo "[$pid] Done. BaseX server for port $portCounter is offline"
    ((portCounter++))
  done
  echo "Shutdown complete. Have a good day."
  exit 0;
}

INPUTDIR=$1
DIRS=$(find $INPUTDIR -mindepth 1 -maxdepth 1 ! -path .)

pids_memory=()
port=1984
portCounter=$port

echo "Start BaseX servers..."

while read -r line; do
  /opt/basex/bin/basexserver -p $portCounter & pids_memory+=("$!")
  printf "[%s] BaseX server started on port %s\n" "${pids_memory[-1]}" "${portCounter}"
  ((portCounter++))
done <<< $DIRS

trap on_exit SIGINT SIGTERM

sleep infinity
