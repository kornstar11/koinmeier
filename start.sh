#!/bin/bash
#JMX_OPTS="$JMX_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"
#JMX_OPTS="$JMX_OPTS -Dcom.sun.management.jmxremote.port=6789"
#JMX_OPTS="$JMX_OPTS -Dcom.sun.management.jmxremote.ssl=false"
#JMX_OPTS="$JMX_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
#JMX_OPTS="$JMX_OPTS -Djava.rmi.server.hostname=72.14.179.232"
JMX_OPTS=""
java -server -Xms512M -Xmx512M -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing $JMX_OPTS -jar /root/exchange2/target/scala-2.11/exchange.jar
