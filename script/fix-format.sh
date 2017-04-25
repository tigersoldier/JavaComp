#!/bin/bash

###
# Format all Java and BUILD files of the project using google-java-format and buildifier.
#
# Usage fix-format.sh [-f|--fix] [-c|--changed-only] [-v|--verbose]
#    -f --fix: If specified, fix the format errors. If not specified, show the format errors and
#              exit.
#    -c --changed-only: If specified, fix only changed files. Otherwise fix all files.
#    -v --verbose: If specified without -f, output files need to be fixed.
###

check_formatters() {
  has_error=$(false)
  if [[ ! $(which google-java-format) ]]; then
      echo "ERROR: google-java-format is not installed."
      has_error=$(true)
  fi
  if [[ ! $(which buildifier) ]]; then
      echo "ERROR: buildifier is not installed."
      has_error=$(true)
  fi
  if [[ $has_error ]]; then
      exit 1
  fi
}

get_project_root_dir() {
  local _script_dir=$(dirname $1)
  echo `cd $_script_dir/..; pwd`
}

parse_args() {
  fix_mode=false
  changed_only=false
  verbose=false
  for arg in "$@"
  do
    case $arg in
      -f|--fix)
        fix_mode=true
        ;;
      -c|--changed_only)
        changed_only=true
        ;;
      -v|--verbose)
        verbose=true
        ;;
      *)
        ;;
    esac
  done
}

run_buildifier() {
  if [[ $# -eq 0 ]]; then
    return 0
  fi
  if [[ $fix_mode = true ]]; then
    local _buildifier_args=""
  else
    local _buildifier_args="-mode=check"
  fi
  local _output=$(buildifier $_buildifier_args "$@")
  if [[ $verbose = true ]]; then
    echo $_output
  fi
  if [[ -z $_output ]]; then
    return 0
  else
    return 1
  fi
}

run_google_java_format() {
  if [[ $# -eq 0 ]]; then
    return 0
  fi
  if [[ $fix_mode = true ]]; then
    google-java-format --replace "$@"
    return 0
  else
    local _ret=0
    for file in $@; do
      if [[ -f $file ]]; then
          google-java-format $file | cmp -s $file -
          if [[ $? -ne 0 ]]; then
              if [[ $verbose = true ]];then
                  echo $file
              fi
              local _ret=1
          fi
      fi
    done
  fi
  return $_ret
}

run_formatters() {
  if [[ $fix_mode = true ]]; then
    local _buildifier_args=""
    local _gjf_args="--replace"
  else
    local _buildifier_args="-mode=check"
    local _gjf_args=""
  fi
  if [[ $changed_only = true ]]; then
    local bazel_files=$(git diff --name-only HEAD | grep -E '(\bBUILD$|\bWORKSPACE$)')
    local java_files=$(git diff --name-only HEAD | grep -E '\.java$' | grep -v "testdata")
  else
    local bazel_files=$(find . | grep -E '(\bBUILD$|\bWORKSPACE$)')
    local java_files=$(find . -name "*.java" | grep -v "testdata")
  fi

  run_buildifier $bazel_files
  buildifier_return=$?
  run_google_java_format $java_files
  gjf_return=$?

  if [[ $buildifier_return = 0 && $gjf_return = 0 ]]; then
    return 0
  else
    return 1
  fi
}

main() {
  check_formatters
  root_dir=$(get_project_root_dir $0)
  parse_args $@
  pushd $root_dir > /dev/null
  run_formatters $1
  local _ret=$?
  popd > /dev/null
  exit $_ret
}

main $@
