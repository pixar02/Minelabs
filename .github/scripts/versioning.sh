#!/bin/bash
set -x
# Increment the version
CURRENT_VERSION=$(grep "mod_version" gradle.properties | cut -d'=' -f2 | tr -d '[:space:]')
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR="${VERSION_PARTS[0]}"
MINOR="${VERSION_PARTS[1]}"
PATCH="${VERSION_PARTS[2]}"
((PATCH++))
NEW_VERSION="$MAJOR.$MINOR.$PATCH"

# Update the version in gradle.properties
sed -i "s/^mod_version=.*/mod_version=$NEW_VERSION/" gradle.properties

# Commit the updated version
git config user.email "github-actions@github.com"
git config user.name "GitHub Actions"
git add gradle.properties
git commit -m "chore: Update mod_version to $NEW_VERSION"

# Tag the commit with the new version
git tag -a "v$NEW_VERSION" -m "Version $NEW_VERSION"

# Push the changes and tag to the default branch
git push origin HEAD
git push origin "v$NEW_VERSION"
