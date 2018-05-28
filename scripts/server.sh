#!/usr/bin/env bash

if [ -z "$1" ] 
  then
    echo "No port specified"
    exit 1
fi

java -Dserver.port=$1 -jar ../target/node-0.0.1-SNAPSHOT.jar 
