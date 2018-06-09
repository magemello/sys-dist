#!/usr/bin/env bash

port=3001
if [ -n "$1" ]
  then
    port=$1
fi

iden=$(echo $port-3000 | bc)
# port=$(( $RANDOM % 3 + 3001 ));
curl -v -H 'Content-Type: application/json' -X POST -d '{"key": "city", "val": "London-in-FAKE", "ts": 11111111}' http://127.0.0.$iden:$port/ap/repair
echo -e "\n"