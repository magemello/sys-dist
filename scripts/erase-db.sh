#!/usr/bin/env bash

if [ -n "$1" ]
  then
	iden=$(echo $1-3000 | bc)
    curl -X POST http://127.0.0.$iden:300$iden/demo/cleandb 1>/dev/null 2>/dev/null &
  else
  	curl -X POST http://127.0.0.1:3001/demo/cleandb 1>/dev/null 2>/dev/null &
	curl -X POST http://127.0.0.2:3002/demo/cleandb 1>/dev/null 2>/dev/null &
	curl -X POST http://127.0.0.3:3003/demo/cleandb 1>/dev/null 2>/dev/null &
	curl -X POST http://127.0.0.4:3004/demo/cleandb 1>/dev/null 2>/dev/null &
	curl -X POST http://127.0.0.5:3005/demo/cleandb 1>/dev/null 2>/dev/null &
fi
