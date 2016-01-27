#!/bin/sh

export BUILD_BASE='/home/oneops/build'
export OO_HOME='/home/oneops'
export SEARCH_SITE=localhost
export GITHUB_URL='https://github.com/oneops'
export GITHUB_CIRCUIT_URL=git@gecgit:walmartlabs

now=$(date +"%T")
echo "Starting at : $now"

source $OO_HOME/install_build_srvr.sh

now=$(date +"%T")
echo "Completed git build : $now"

source $OO_HOME/init_db.sh

now=$(date +"%T")
echo "Deploying artifacts: $now "

cd $OO_HOME/dist
tar -xzvf oneops-continuous.tar.gz

cd $OO_HOME

export RAILS_ENV=development
export OODB_USERNAME=kloopz
export OODB_PASSWORD=kloopz

source $OO_HOME/deploy_display.sh

source $OO_HOME/deploy_amq.sh

source $OO_HOME/deploy_java.sh

source $OO_HOME/deploy_search.sh

source $OO_HOME/deploy_ooadmin.sh

cd /opt/oneops

#nohup rails server >> /opt/oneops/log/rails.log 2>&1 &
service display start

echo "OneOps should be up on http://localhost:3000"
echo "Configure your port forwarding and shut down iptables service (or configure it) if needed"
