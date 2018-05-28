#!/usr/bin/env bash

if [ -z "$1" ]
  then
    echo "No port specified"
    exit 1
fi

iden=$(echo $1-3000 | bc)
echo Going to start 127.0.0.$iden:$1

cd ../node
mvn -Dserver.address=127.0.0.$iden -Dserver.port=$1 spring-boot:run
