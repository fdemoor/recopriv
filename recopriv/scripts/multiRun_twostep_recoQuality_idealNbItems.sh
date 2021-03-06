#!/bin/bash 
# Run a simulation using the parameters in $BASEDIR/scripts/exportParameterVariables.sh

############################################################
##### Find where this script is stored and set BASEDIR #####
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export BASEDIR="$(dirname $DIR)"

export CLASSPATH=".:$BASEDIR/bin/:$BASEDIR/lib/*"

###################################################
##### Checking the helper scripts are present #####
if [[ ! -f $BASEDIR/scripts/exportParameterVariables.sh ]]; then
 echo "exportParameterVariables.sh is missing";
 exit
fi
if [[ ! -x $BASEDIR/scripts/generateParametersString.sh ]]; then
 echo "generateParametersString.sh is missing";
 exit
fi

#########################################
##### Setting simulation parameters #####
export baseDir="$BASEDIR"
source $BASEDIR/scripts/exportParameterVariables.sh

#vcsVersionId=`svnversion $BASEDIR`
vcsVersionId=$(cd "$BASEDIR" && git rev-parse HEAD)

SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)

####################################
##### Maximum JVM memory usage #####
#JVM_MEMORY=32768 # in Mio
#JVM_MEMORY=24576 # in Mio
#JVM_MEMORY=16384 # in Mio
#JVM_MEMORY=8192 # in Mio
JVM_MEMORY=6144 # in Mio

####################################################################
##### Building the line of arguments for the execution command #####
#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M SampleRecommender"
#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M recoPrivResearch.EvaluateRecommender"
COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M -Dlog4j.configurationFile="$BASEDIR/scripts/log4j2.xml" recoPrivResearch.EvaluateRecommender"
#COMMAND_ARGUMENTS="-Xmx${JVM_MEMORY}M -XX:OnOutOfMemoryError="kill -9 %p" recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=cpu.txt,cpu=samples,interval=1000,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=heap.txt,heap=sites,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"

#################################################################################
# Reco quality with twostep and twostepglobal measures and varying idealNbItems #
#################################################################################
# FYI, available parameters are
seed=862576678
#baseDir=$HOME/Workspace/cf_knn_sybil_attack/example_recsys_mahout
#log4jConfFile=$baseDir/scripts/log4j.properties
#datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv
#testingPath=$baseDir/datasets/movieLens/ml-100k/u.test
outputPath=$BASEDIR/output/
#experimentId=0
#vcsVersionId=unknown
similarityType=cosine
twostepIdealNbItems=400
k=10
#nbAttackers=1
#nbTargets=943
nbTargets=100
targetID=130
likeThreshold=3
useSequentialEvaluators=true
computeMae=false
computeRmse=false
computeIRStats=false
doSybilAttack=true
#notRatedItemConsideredDisliked=true
r=10
trainingPercentage=0.8
validationPercentage=1.0
relativeNbAuxItems=true
nbAuxiliaryItems=20
percentAuxiliaryItems=0.5
#nbSybils=10
sybilRatingsStrategy=groundtruth
adaptiveSybils=false
adaptiveSybilsNbRounds=1
sybilsNbRecoPerRound=5
neighborChoiceBehavior=random

prevWD=$(pwd)

mkdir -p $outputPath
cd $outputPath

expeDir="recoQuality_post5c28f2a77cf2ced61c41e899289546315a40e2b3/"
mkdir -p $expeDir
cd $expeDir

for twostepIdealNbItems in 25 50 100 200 400 500; do 
   for similarityType in "twostep(0.8)" "twostepglobal(0.8)"; do
	dir="${similarityType}/idealNbItems${twostepIdealNbItems}"
	mkdir -p $dir
	cd $dir

	computeMae=false
	computeRmse=true
	computeIRStats=false
	doSybilAttack=false
	for trainingPercentage in 0.9 ; do
		for k in 1 3 5 10 15 20 30 40 50 75 100; do
			seed=862576678
			for seed in $(seq $seed $(($seed + 19))); do
				SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)
				java $COMMAND_ARGUMENTS $SIMULATION_PARAMETERS
			done
		done
	done

	cd $outputPath/$expeDir
    done
done


cd $prevWD
