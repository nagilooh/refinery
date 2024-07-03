#!/bin/bash

cd Z: || exit
mkdir -p "models/dolt"
cd models/dolt || exit
dolt config --global --add user.email "trainbenchmark@refinery.services"
dolt config --global --add user.name "trainbenchmark"
dolt init
#echo "MySQL started"
