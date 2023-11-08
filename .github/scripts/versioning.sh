#!/bin/bash -e

. <(grep mod_version gradle.properties)
echo $mod_version