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
#JVM_MEMORY=6144 # in Mio
JVM_MEMORY=2144 # in Mio

####################################################################
##### Building the line of arguments for the execution command #####
#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M SampleRecommender"
#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M recoPrivResearch.EvaluateRecommender"
COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M -Dlog4j.configurationFile="$BASEDIR/scripts/log4j2.xml" recoPrivResearch.EvaluateRecommender"
#COMMAND_ARGUMENTS="-Xmx${JVM_MEMORY}M -XX:OnOutOfMemoryError="kill -9 %p" recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=cpu.txt,cpu=samples,interval=1000,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=heap.txt,heap=sites,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"

##################################################################################
# Non-adaptive Sybil attack, random auxiliary information, with cosine on ml100k #
##################################################################################
# FYI, available parameters are
#baseDir=$HOME/Workspace/cf_knn_sybil_attack/example_recsys_mahout
#log4jConfFile=$baseDir/scripts/log4j.properties
datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv
#datasetPath=$baseDir/datasets/movieLens/ml1m/ratings.csv
#datasetPath=$baseDir/datasets/jester/jester-data-1.csv
#datasetPath=$baseDir/datasets/movietweetings_recsys_challenge_2014/training_test_eval_concat_cleaned.csv

seed=862576678
log4jConfFile=$baseDir/scripts/log4j.properties
#datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv
testingPath=$baseDir/datasets/movieLens/ml-100k/u.test
outputPath=$BASEDIR/output/
experimentId=0
vcsVersionId=unknown
similarityType="twostep(0.8)"
twostepUseThreshold=true
twostepIdealNbItems=1682
#twostepIdealNbItems=100
twostepAttack=5
nbExtraItemPerSybil=50
twostepFirstStepType=cosine
isGlobalIdealNbItems=true
percentIdealNbItems=1.0
randomizeIdealNbItems=true
percentAdditionalItems=1.0
k=10
neighborhoodType=knn
likeThreshold=3.0
trainingPercentage=0.8
validationPercentage=1.0
useSequentialEvaluators=true
computeMae=false
computeRmse=false
computeIRStats=false
r=10
doSybilAttack=true
nbAttackers=1
nbTargets=1
targetID=1
notRatedItemConsideredDisliked=true
relativeNbAuxItems=true
nbAuxiliaryItems=20
percentAuxiliaryItems=0.04
auxiliaryItemsSelectionStrategy=bestRated
bestWorstFrac=2
nbSybils=10
sybilRatingsStrategy=groundtruth
adaptiveSybils=false
adaptiveSybilsNbRounds=10
sybilsNbRecoPerRound=5
neighborChoiceBehavior=random
doOnlyPrecomputations=false

prevWD=$(pwd)

mkdir -p $outputPath
cd $outputPath

expeDir="ml100k/thresholdAnalysis"
mkdir -p $expeDir
cd $expeDir


function runSimu {
	currentExpe="${similarityType}/percentAuxItems_${percentAuxiliaryItems}"
	mkdir -p $currentExpe
	cd $currentExpe
	SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)
	java $COMMAND_ARGUMENTS $SIMULATION_PARAMETERS
	cd $outputPath/$expeDir
}


#for targetID in $(seq 1 943); do
#	for percentAuxiliaryItems in 0.02 0.03 0.04 0.05 0.06 0.07 0.08 0.09 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0; do
#		runSimu &
#	done
#done

i=$1
while [ "$i" -lt "$2" ]; do
	j=$(($i+$3))
	for thres in 1.0 0.95 0.90 0.85 0.80 0.75 0.7 0.65 0.6; do
		similarityType="twostep($thres)"
		for percentAuxiliaryItems in 0.02 0.03 0.04 0.05 0.06 0.07 0.08 0.09 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0; do
			for targetID in $(seq ${i} ${j}); do
				runSimu &
			done
			wait
		done
	done
	i=$(($i+$3+1))
done


cd $prevWD
