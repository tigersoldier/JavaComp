#!/bin/bash

pushd $(dirname $0)/..
ROOT_DIR=$(pwd)
JDK_CLASSES_DIR=$ROOT_DIR/openjdk_src/src/share/classes/

function clone {
    test -d openjdk_src && rm -r openjdk_src
    hg clone http://hg.openjdk.java.net/jdk8u/jdk8u/jdk openjdk_src
}

function index {
    echo Indexing $JDK_CLASSES_DIR
    bazel run src/main/java/org/javacomp/tool:Indexer -- $JDK_CLASSES_DIR /tmp/index.json
}

parse_args() {
    skip_clone=false
    for arg in "$@"
    do
        case $arg in
            -s|--skip-clone)
                skip_clone=true
                ;;
        esac
    done
}

function main {
    parse_args $@

    if [[ $skip_clone = false ]]; then
        clone
    fi
    index
}

main $@

popd
