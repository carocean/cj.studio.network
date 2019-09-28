#!/bin/sh
#shell call java program
#echo $JAVA_HOME
java -Xms50m -Xmx850m -XX:MaxDirectMemorySize=512m -jar  peer-1.0.0.jar $1 $2 $3 $4 $5 $6 $7 $8 $9 ${10} ${11} ${12} ${13} ${14} ${15}
exit 0;
