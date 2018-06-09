#!/usr/bin/env bash

if [ -z "$1" ]
  then
    echo "No port specified"
    exit 1
fi

iden=$(echo $1-3000 | bc)
echo Going to freeze 127.0.0.$iden

sudo echo "block drop in on lo0 proto tcp from any to any port = 300$iden" >> ~/sub.filter.conf
sudo echo "block drop out on lo0 proto tcp from 127.0.0.$iden to any" >> ~/sub.filter.conf
sudo pfctl -f /etc/pf.conf