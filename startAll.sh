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

java peerProcess 1001 &
java peerProcess 1002 &
java peerProcess 1003 &
java peerProcess 1004 &
java peerProcess 1005 &
