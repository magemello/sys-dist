#!/usr/bin/env bash
# port=$(( $RANDOM % 3 + 3001 ));
port=3001
if [ -n "$1" ]
  then
    port=$1
fi
iden=$(echo $port-3000 | bc)

CMD="curl -v  http://127.0.0.$iden:$port/storage/city"
echo -e "\n"
echo $CMD
$CMD
echo -e "\n"
