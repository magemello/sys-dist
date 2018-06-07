#!/usr/bin/env bash

port=3001
if [ -n "$1" ]
  then
    port=$1
fi

iden=$(echo $1-3000 | bc)
year=$(( $RANDOM % 100 + 1996 ))
# port=$(( $RANDOM % 3 + 3001 ));
CMD="curl -v -X POST http://127.0.0.$iden:$port/storage/city/London-in-$year"
echo -e "\n"
echo $CMD
$CMD
echo -e "\n"
