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
JVM_MEMORY=16384 # in Mio
#JVM_MEMORY=8192 # in Mio
#JVM_MEMORY=6144 # in Mio

####################################################################
##### Building the line of arguments for the execution command #####
#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M SampleRecommender"
#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M recoPrivResearch.EvaluateRecommender"
COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M -Dlog4j.configurationFile="$BASEDIR/scripts/log4j2.xml" recoPrivResearch.EvaluateRecommender"
#COMMAND_ARGUMENTS="-Xmx${JVM_MEMORY}M -XX:OnOutOfMemoryError="kill -9 %p" recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=cpu.txt,cpu=samples,interval=1000,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=heap.txt,heap=sites,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"

#################################################################################################
# Non-adaptive Sybil attack, one target attacked at a time, with twostep80 varying idealNbItems #
#################################################################################################
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
neighborhoodType=knn
#nbAttackers=1
nbTargets=943
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
doOnlyPrecomputations=false

prevWD=$(pwd)

mkdir -p $outputPath
cd $outputPath

datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv
k=10
nbSybils=$k
computeMae=false
computeRmse=false
computeIRStats=false
doSybilAttack=true
adaptiveSybils=false
sybilsNbRecoPerRound=5
nbTargets=1
likeThreshold=3
similarityType="twostep(0.8)"

expeDir="ml100k/non-adaptive/one_target_at_a_time/${similarityType}/"
mkdir -p $expeDir
cd $expeDir

for targetID in $(seq 1 943); do
	for twostepIdealNbItems in 25 50 100 200 400 500; do 
		dir="idealNbItems${twostepIdealNbItems}"
		mkdir -p $dir
		cd $dir

		for percentAuxiliaryItems in 0.1 0.2 0.3 0.5 0.7 0.8 0.9; do
			currentExpe="percentAuxItems_${percentAuxiliaryItems}"
			mkdir -p $currentExpe
			cd $currentExpe

			#seed=862576678
			#for seed in $(seq $seed $(($seed + 19))); do # = 20 runs
				SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)
				java $COMMAND_ARGUMENTS $SIMULATION_PARAMETERS
			#done

			cd ../
		done

		cd $outputPath/$expeDir
	done
done


cd $prevWD
