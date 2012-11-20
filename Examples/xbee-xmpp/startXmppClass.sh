#!/bin/bash

# use this script to run any class that has a main method
# make sure eclipse has built all the classes in the ./bin folder
# first make it executable: chmod u+x startXmppClass.sh
# example usage: ./startXmppClass.sh com.rapplogic.xbee.xmpp.examples.XBeeXmppGatewayExample

filelist=`find lib -name '*.jar'`

for file in $filelist
do
	echo "adding $file to classpath"
	classpath=$classpath:$file
done

classpath=$classpath:bin

java -classpath $classpath -Djava.library.path=. $@

