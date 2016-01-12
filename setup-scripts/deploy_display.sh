#!/bin/sh


echo "Deploying Display component: $now "

mkdir -p /opt/oneops

mv $OO_HOME/dist/oneops/dist/app.tar.gz /opt/oneops/app.tar.gz

cd /opt/oneops

tar -xzvf app.tar.gz

bundle install

rake db:setup
rake db:migrate

now=$(date +"%T")

cp $OO_HOME/start-display.sh /opt/oneops

chmod +x /opt/oneops/start-display.sh

echo "Done with Display: $now "


