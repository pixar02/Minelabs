#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <main_version> <merge_version>"
    exit 1
fi

main_version=$1
merge_version=$2

echo "main version: $main_version"
echo "branch version: $merge_version"

IFS='.' read -ra mainV <<< "$main_version"
IFS='.' read -ra mergeV <<< "$merge_version"

function check() {
    for ((i=0; i<=2; i++)); do
        if (( ${mergeV[$i]} > ${mainV[$i]} )); then
          echo "MERGE IS BIGGER" 
          return 0
        fi
    done
    return 1
}

if check; then
  v=$((mainV[2] + 1))
  builder="${mainV[0]}.${mainV[1]}.$v"
  echo "auto: "
  echo "$builder"
  sed -i "s/mod_version=$main_version/mod_version=$builder/" gradle.properties
else
    echo "merge"
    echo "$merge_version"
fi
