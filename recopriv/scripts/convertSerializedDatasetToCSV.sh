#!/bin/bash 
# Use the SerializedDatasetToCSV java class to convert all objects (path) provided as arguments of this script.
# Converted CSV files are written in the same directories as the original objects.

# Find where this script is stored and set BASEDIR
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export BASEDIR="$(dirname $DIR)"

export CLASSPATH=".:$BASEDIR/bin/:$BASEDIR/lib/*"

COMMAND_ARGUMENTS="-Dlog4j.configurationFile="$BASEDIR/scripts/logInfoLevelToConsole-log4j2.xml" recoPrivResearch.tools.SerializedDatasetToCSV"

java $COMMAND_ARGUMENTS "$@"

