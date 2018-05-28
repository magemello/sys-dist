#!/usr/bin/env bash
# port=$(( $RANDOM % 3 + 3001 ));
port=3001
CMD="curl -v  http://localhost:$port/storage/city"
echo -e "\n"
echo $CMD
$CMD
echo -e "\n"
