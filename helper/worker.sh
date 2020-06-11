#!/bin/sh

arg="$*"
echo "[$$] start indexing $arg"

dbname="${arg##*/}"
programout=$(basexclient -V -Uadmin -Padmin -c "CREATE DB $dbname $arg")
echo "[$$] $programout"
echo "[$$] finished indexing."
