#!/bin/bash
function on_exit() {
  echo "[$BASHPID] Bye bye."
  exit;
}

echo "[$BASHPID] Waked up..."

trap on_exit SIGINT SIGTERM

while true
do
    echo "[$BASHPID] zZzZ..."
    sleep 1
done
