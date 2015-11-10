#!/bin/bash
if [ $# -lt 1 ]
then
echo "need parameters as : <ipConfigserver>+"
else
apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
echo 'deb http://downloads-distro.mongodb.org/repo/debian-sysvinit dist 10gen' | sudo tee /etc/apt/sources.list.d/mongodb.list
apt-get update
apt-get install -y mongodb-org

service mongod stop

var=$1
shift 1
for configServerIP in $* 
do
var="$var,$configServerIP:27019"
done

mongos --configdb $var --fork --logpath /var/log/mongodb/mongod.log
sleep 20s
mongo < ./addShardConfig.txt



fi
