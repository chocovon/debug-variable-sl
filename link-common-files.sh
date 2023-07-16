#!/bin/sh

# hard links common files.
# todo: common module later.

rm -f ./inner-tool/src/main/java/common/*
find ./src/main/java/common -type file -exec ln {} ./inner-tool/{} \;
