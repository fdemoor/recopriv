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
seed=862576678
#baseDir=$HOME/Workspace/cf_knn_sybil_attack/example_recsys_mahout
#log4jConfFile=$baseDir/scripts/log4j.properties
datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv
#datasetPath=$baseDir/datasets/movieLens/ml1m/ratings.csv
#datasetPath=$baseDir/datasets/jester/jester-data-1.csv
#datasetPath=$baseDir/datasets/movietweetings_recsys_challenge_2014/training_test_eval_concat_cleaned.csv

similarityType="twostep(0.8)"
twostepUseThreshold=true
twostepIdealNbItems=1682
twostepAttack=0
nbExtraItemPerSybil=50
twostepFirstStepType=cosine
isGlobalIdealNbItems=true
percentIdealNbItems=1.0
randomizeIdealNbItems=false
variantIdealNbItems=u
k=10
neighborhoodType=knn
likeThreshold=3.0
trainingPercentage=0.8
validationPercentage=1.0
useSequentialEvaluators=true
computeMae=false
computeRmse=false
computeIRStats=true
r=10
doSybilAttack=false
nbAttackers=1
nbTargets=1
targetID=13
notRatedItemConsideredDisliked=true
relativeNbAuxItems=true
nbAuxiliaryItems=20
percentAuxiliaryItems=0.5
auxiliaryItemsSelectionStrategy=random
nbSybils=10
sybilRatingsStrategy=groundtruth
adaptiveSybils=false
adaptiveSybilsNbRounds=10
sybilsNbRecoPerRound=5
neighborChoiceBehavior=random
doOnlyPrecomputations=false

outputPath=$BASEDIR/output/

prevWD=$(pwd)

mkdir -p $outputPath
cd $outputPath

#datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv

expeDir="ml100k/IR"
mkdir -p $expeDir
cd $expeDir


function runSimu {
	currentExpe="${similarityType}/r_${r}"
	mkdir -p $currentExpe
	cd $currentExpe
	SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)
	java $COMMAND_ARGUMENTS $SIMULATION_PARAMETERS
	cd $outputPath/$expeDir
}

for similarityType in "cosine-average" "jaccard" "pearson" "wupXontoY" "wupYontoX" "cosine" "twostep(0.8)" "cosine-mahout"; do
	runSimu &
done
wait



cd $prevWD
