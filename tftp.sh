#!/bin/bash

HOST=""
FILE=""
SERVER="server"
if [ "$1" = "$SERVER" ] ; then
    sbt 'runMain edu.oswego.cs.gmaldona.RemoteMachine'
    exit 1

fi

if ping -c 1 -W 1 "$1"; then
  HOST="$1"
  FILE="$2"
  clear

  sbt "runMain edu.oswego.cs.gmaldona.LocalMachine $HOST $FILE"

elif ping -c 1 -W 1 "$2"; then
    FILE="$1"
    HOST="$2"
    clear
    sbt "runMain edu.oswego.cs.gmaldona.LocalMachine $FILE $HOST"
else
    echo "Unknown Host"
fi



