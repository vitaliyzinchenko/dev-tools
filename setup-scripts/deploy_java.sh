#!/bin/sh

now=$(date +"%T")

echo "Deploying Tomcat web apps: $now "

mkdir -p /usr/local/oneops/certs

if [ ! -e /usr/local/oneops/certs/oo.key ]; then
cd /usr/local/oneops/certs
dd if=/dev/urandom count=24 bs=1 | xxd -ps > oo.key
truncate -s -1 oo.key
chmod 400 oo.key
chown tomcat7:root oo.key
fi

cd /usr/local/tomcat7/webapps/

service tomcat7 stop

rm -rf *

cp $OO_HOME/dist/oneops/dist/*.war /usr/local/tomcat7/webapps

cp $OO_HOME/tom_setenv.sh /usr/local/tomcat7/bin/setenv.sh
chown tomcat7:root /usr/local/tomcat7/bin/setenv.sh

mkdir -p /opt/oneops/controller/antenna/retry
mkdir -p /opt/oneops/opamp/antenna/retry
mkdir -p /opt/oneops/cms-publisher/antenna/retry

service tomcat7 start

now=$(date +"%T")
echo "Done with Tomcat: $now "


