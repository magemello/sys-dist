#!/usr/bin/env bash
if [ -z "$1" ]
  then
    echo "No port specified"
    exit 1
fi

iden=$(echo $1-3000 | bc)
reg='/300'$iden'/d'
echo $reg
echo Going to unfreeze 127.0.0.$iden

sed -i '' '/300'$iden'/d' ~/sub.filter.conf
sed -i '' '/127.0.0.'$iden'/d' ~/sub.filter.conf
sudo pfctl -f /etc/pf.conf