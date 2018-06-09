#!/usr/bin/env bash

if [ -z "$1" ]
  then
    echo "No port specified"
    exit 1
fi

delay=""
if [ $1 = "3004" ]; then
    delay="-Dserver.delay=1500"
fi

if [ -n "$2" ]
  then
    delay="-Dserver.delay=$2"
fi

iden=$(echo $1-3000 | bc)
echo Going to start 127.0.0.$iden:$1

cd ../node
java $delay -Dserver.address=127.0.0.$iden -Dserver.port=$1 -jar target/node-0.0.1-SNAPSHOT.jar
