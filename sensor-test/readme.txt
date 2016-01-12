Executable JAR
--------------

This is a little project to generate metrics flow.
Unzip it somewhere, run mvm package Ð this will create sensor-test-fat.jar then run it like this:

java -jar sensor-test-fat.jar ciId=370479 manifestId=224828 poolSize=2 source=tom-compute-cpu channel=my-channel bucket=1m metric=CpuIdle value=70 tries=5 sleep=3000

Params pretty much self explanatory
