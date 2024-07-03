#!/bin/bash

cd Z: || exit
cd models/dolt || exit
dolt add .
dolt commit --allow-empty -m "trainbenchmark"
