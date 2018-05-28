#!/usr/bin/env bash
curl http://127.0.0.1:3001/storage/dump 1>/dev/null 2>/dev/null &
curl http://127.0.0.2:3002/storage/dump 1>/dev/null 2>/dev/null &
curl http://127.0.0.3:3003/storage/dump 1>/dev/null 2>/dev/null &
curl http://127.0.0.4:3004/storage/dump 1>/dev/null 2>/dev/null &
curl http://127.0.0.5:3005/storage/dump 1>/dev/null 2>/dev/null &
