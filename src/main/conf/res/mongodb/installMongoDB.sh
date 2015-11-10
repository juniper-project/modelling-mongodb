#!/bin/bash

apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
echo 'deb http://downloads-distro.mongodb.org/repo/debian-sysvinit dist 10gen' | sudo tee /etc/apt/sources.list.d/mongodb.list
apt-get update
apt-get install -y mongodb-org

service mongod stop

mongod --smallfiles --dbpath /var/lib/mongodb --fork --logpath /var/log/mongodb.log
sleep 20s
mongo << EOF
exit
EOF


