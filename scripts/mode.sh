#!/usr/bin/env bash
curl -X POST http://127.0.0.1:3001/demo/mode/$1 1>/dev/null 2>/dev/null &
curl -X POST http://127.0.0.2:3002/demo/mode/$1 1>/dev/null 2>/dev/null &
curl -X POST http://127.0.0.3:3003/demo/mode/$1 1>/dev/null 2>/dev/null &
curl -X POST http://127.0.0.4:3004/demo/mode/$1 1>/dev/null 2>/dev/null &
curl -X POST http://127.0.0.5:3005/demo/mode/$1 1>/dev/null 2>/dev/null &
