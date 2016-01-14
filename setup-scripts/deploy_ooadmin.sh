#!/bin/sh

echo "Deploying OneOps Admin "

gem install  $OO_HOME/dist/oneops/dist/oneops-admin-1.0.0.gem --no-ri --no-rdoc

mkdir -p /opt/oneops-admin
cd /opt/oneops-admin

rm -fr circuit
circuit create
cd circuit

bundle install
if [ ! $? -eq 0 ]; then
	echo "bundle install failed, retrying"
	bundle install
	if [ ! $? -eq 0 ]; then
		echo "Can not instal oneops-admin circuit, exiting :-("
		exit 1
	fi
fi

circuit init

cd "$BUILD_BASE"

if [ -d "$BUILD_BASE/circuit-oneops-1" ]; then
  echo "doing git pull on circuit-oneops-1"
  cd "$BUILD_BASE/circuit-oneops-1"
  git pull
else
  echo "doing git clone"
  git clone "$GITHUB_URL/circuit-oneops-1.git"
fi
sleep 2

cd "$BUILD_BASE/circuit-oneops-1"
bundle install
circuit install

mkdir -p /opt/oneops/public/images/cms
mkdir -p /opt/oneops/public/images/pack

for img in $BUILD_BASE/circuit-oneops-1/components/cookbooks/*/doc/*.png
do
   cp "${img}" /opt/oneops/public/images/cms
done

for img in $BUILD_BASE/circuit-oneops-1/packs/doc/*.png
do
   cp "${img}" /opt/oneops/public/images/pack
done

echo "Done with admin"

echo "install inductor"

cd /opt/oneops
inductor create

\cp /opt/activemq/conf/client.ts /opt/oneops/inductor/lib

echo "done with inductor"
