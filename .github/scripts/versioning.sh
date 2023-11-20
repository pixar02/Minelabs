#!/bin/bash

base_version=$1
merged_version=$2

if [[ "$base_version" == "$merged_version" ]]; then
  new_version=$((merged_version + 1))
  echo "Updating version to $new_version"
  sed -i "s/version=$merged_version/version=$new_version/" gradle.properties
else
  echo "Merged version is greater, keeping it."
fi
