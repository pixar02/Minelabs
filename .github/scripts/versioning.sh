#!/bin/bash

main_version=$1
merge_version=$2

IFS='.' read -ra mainV <<< "$main_version"
IFS='.' read -ra mergeV <<< "$merge_version"

check() {
    for ((i=2; i>=0; i--)); do
        base="${mainV[$i]}"
        merge="${mergeV[$i]}"
        if ((merge > base)); then
            return 0
        fi
    done
    return 1
}

if [ $(check) == 1 ]; then
  v=$((mainV[2] + 1))
  builder="${mainV[0]}.${mainV[1]}.$v"
  echo "auto: "
  echo "$builder"
  sed -i "s/mod_version=$main_version/mod_version=$builder/" gradle.properties
else 
    echo "merge"
    echo "$merge_version" 
fi

