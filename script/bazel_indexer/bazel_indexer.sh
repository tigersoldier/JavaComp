#!/bin/bash

## Usage: bazel_indexer.sh <build target>

ASPECT_ROOT=$(dirname $0)
BUILD_TARGET=$1

echo aspect_root=$ASPECT_ROOT
echo bazel build $BUILD_TARGET

function bazel_realpath {
    # macOS doesn't have realpath :(
    pwd=$PWD
    cd $1
    echo $PWD
    cd $pwd
}

bazel build $BUILD_TARGET \
      --override_repository=local_javacomp_aspect=$(bazel_realpath $ASPECT_ROOT) \
      --aspects=@local_javacomp_aspect//:aspect.bzl%javacomp_aspect \
      --output_groups=ide-info-text,ide-resolve,-_,-defaults \
      --experimental_show_artifacts 2>&1 | grep "\\.json$" | sed "s/^>>>//"
