#!/usr/bin/env bash

if [ -z "$1" ] 
  then
    echo "No port specified"
    exit 1
fi

cd ../node
mvn -Dserver.port=$1 spring-boot:run
