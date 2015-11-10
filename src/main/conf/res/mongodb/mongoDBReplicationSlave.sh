#!/bin/bash

if [ ! $# -eq 1 ]
then
echo "need parameter as : <replSetName> "
else
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
echo 'deb http://downloads-distro.mongodb.org/repo/debian-sysvinit dist 10gen' | sudo tee /etc/apt/sources.list.d/mongodb.list
apt-get update
apt-get install -y mongodb-org
chmod 777 /etc/mongod.conf
echo smallfiles=true >> /etc/mongod.conf
service mongod stop
mongod --dbpath /var/lib/mongodb --replSet $1 --smallfiles > logMongod.txt &

fi
