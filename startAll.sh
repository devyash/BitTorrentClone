#!/usr/bin/env bash

rm -r peer_1002/
rm -r peer_1003/
rm -r peer_1004/
rm -r peer_1005/
rm -r peer_1006/
rm log_peer_1001.log
rm log_peer_1002.log
rm log_peer_1003.log
rm log_peer_1004.log
rm log_peer_1005.log
rm log_peer_1001.log.lck
rm log_peer_1002.log.lck
rm log_peer_1003.log.lck
rm log_peer_1004.log.lck
rm log_peer_1005.log.lck

mvn exec:java -f pom.xml  -Dexec.mainClass="edu.ufl.cise.cnt5106c.peerProcess" -Dexec.args="1001" &
mvn exec:java -f pom.xml  -Dexec.mainClass="edu.ufl.cise.cnt5106c.peerProcess" -Dexec.args="1002" &
mvn exec:java -f pom.xml  -Dexec.mainClass="edu.ufl.cise.cnt5106c.peerProcess" -Dexec.args="1003" &
mvn exec:java -f pom.xml  -Dexec.mainClass="edu.ufl.cise.cnt5106c.peerProcess" -Dexec.args="1004" &
mvn exec:java -f pom.xml  -Dexec.mainClass="edu.ufl.cise.cnt5106c.peerProcess" -Dexec.args="1005" &
mvn exec:java -f pom.xml  -Dexec.mainClass="edu.ufl.cise.cnt5106c.peerProcess" -Dexec.args="1006"