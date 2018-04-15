#!/bin/bash 
# Generate helper scripts to handle simulation parameters in a shell script

# Find where this script is stored and set BASEDIR
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export BASEDIR="$(dirname $DIR)"

export CLASSPATH=".:$BASEDIR/bin/:$BASEDIR/lib/*"

COMMAND_ARGUMENTS="-Dlog4j.configurationFile="$BASEDIR/scripts/logInfoLevelToConsole-log4j2.xml" recoPrivResearch.tools.Parameters"

java $COMMAND_ARGUMENTS

chmod +x generateParametersString.sh
# Remove definition of variable baseDir in exportParameterVariables.sh as it will be set in run.sh
sed -i '/export baseDir=/d' exportParameterVariables.sh

if [[ $(pwd) != $BASEDIR/scripts ]]; then
	mv exportParameterVariables.sh $BASEDIR/scripts/
	mv generateParametersString.sh $BASEDIR/scripts/
	echo "Moved exportParameterVariables.sh and generateParametersString.sh to $BASEDIR/scripts/"
fi
