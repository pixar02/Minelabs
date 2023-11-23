#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <version>"
    exit 1
fi

main_version=$1

echo "version: $main_version"

IFS='.' read -ra mainV <<< "$main_version"

v=$((mainV[2] + 1))
builder="${mainV[0]}.${mainV[1]}.$v"
echo "$builder"
sed -i "s/mod_version=$main_version/mod_version=$builder/" gradle.properties

