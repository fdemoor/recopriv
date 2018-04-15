#!/bin/bash 
# Generate cumulative distribution of user similarities w/ different measures.
# Usage: ./computeSimilarityDistribution.sh path/to/log4j.properties path/to/dataset.csv <OUTPUT_FILENAME_PREFIX> path/to/result/dir/ {true,false}
# The dataset must be in Mahout-compatible CSV.
# path/to/result/dir/ must already exist.
# The last boolean argument enables logging of similarity values

# Find where this script is stored and set BASEDIR
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export BASEDIR="$(dirname $DIR)"

export CLASSPATH=".:$BASEDIR/bin/:$BASEDIR/lib/*"

# Maximum JVM memory usage
JVM_MEMORY=32768 # in Mio
#JVM_MEMORY=24576 # in Mio
#JVM_MEMORY=16384 # in Mio
#JVM_MEMORY=8192 # in Mio
#JVM_MEMORY=6144 # in Mio

COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M -Dlog4j.configurationFile="$BASEDIR/scripts/logInfoLevelToConsole-log4j2.xml" recoPrivResearch.tools.SimilarityDistribution"

java $COMMAND_ARGUMENTS "$@"

