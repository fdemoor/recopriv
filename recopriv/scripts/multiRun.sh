#!/bin/bash 
# Run a simulation using the parameters in $BASEDIR/scripts/exportParameterVariables.sh

# Find where this script is stored and set BASEDIR
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export BASEDIR="$(dirname $DIR)"

export CLASSPATH=".:$BASEDIR/bin/:$BASEDIR/lib/*"

# Checking the helper scripts are present
if [[ ! -f $BASEDIR/scripts/exportParameterVariables.sh ]]; then
 echo "exportParameterVariables.sh is missing";
 exit
fi
if [[ ! -x $BASEDIR/scripts/generateParametersString.sh ]]; then
 echo "generateParametersString.sh is missing";
 exit
fi

# Setting simulation parameters
export baseDir="$BASEDIR"
source $BASEDIR/scripts/exportParameterVariables.sh
#vcsVersionId=`svnversion $BASEDIR`
vcsVersionId=$(cd "$BASEDIR" && git rev-parse HEAD)
SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)

# Maximum JVM memory usage
#JVM_MEMORY=32768 # in Mio
#JVM_MEMORY=24576 # in Mio
#JVM_MEMORY=16384 # in Mio
#JVM_MEMORY=8192 # in Mio
JVM_MEMORY=6144 # in Mio

#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M SampleRecommender"
#COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M recoPrivResearch.EvaluateRecommender"
COMMAND_ARGUMENTS="-Xms${JVM_MEMORY}M -Xmx${JVM_MEMORY}M -Dlog4j.configurationFile="$BASEDIR/scripts/log4j2.xml" recoPrivResearch.EvaluateRecommender"
#COMMAND_ARGUMENTS="-Xmx${JVM_MEMORY}M -XX:OnOutOfMemoryError="kill -9 %p" recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=cpu.txt,cpu=samples,interval=1000,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"
#COMMAND_ARGUMENTS="-agentlib:hprof=file=heap.txt,heap=sites,depth=200 -Xmx${JVM_MEMORY}M recoEval.Simulator"

###########
# Reco quality and non-adaptive Sybils, 1 target at a time, both with cosine and cosine-mahout
###########
# FYI, available parameters are
seed=862576678
#baseDir=$HOME/Workspace/cf_knn_sybil_attack/example_recsys_mahout
#log4jConfFile=$baseDir/scripts/log4j.properties
#datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv
#datasetPath=$baseDir/datasets/jester/jester-data-1.csv
#datasetPath=$baseDir/datasets/movietweetings_recsys_challenge_2014/training_test_eval_concat_cleaned.csv
outputPath=$BASEDIR/output/
#experimentId=0
#vcsVersionId=unknown
#similarityType=cosine-mahout
similarityType=cosine
#similarityType=cosine-average
k=10
neighborhoodType=knn
#neighborhoodType=random
#nbAttackers=1
#nbTargets=943 # all ml100k users
#nbTargets=24983 # all jester users
#nbTargets=24924 # all movietweetings users
nbTargets=1
targetID=0
likeThreshold=3
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
percentAuxiliaryItems=0.9
nbSybils=10
##sybilRatingsStrategy=neutral
sybilRatingsStrategy=groundtruth
adaptiveSybils=false
adaptiveSybilsNbRounds=5
sybilsNbRecoPerRound=5
#neighborChoiceBehavior=lower
neighborChoiceBehavior=random
#neighborChoiceBehavior=higher

prevWD=$(pwd)

cd $outputPath

#datasetPath=$baseDir/datasets/jester/jester-data-1.csv
#expeDir="test/jester/recoQuality/${neighborhoodType}Neighbors/"
#mkdir -p $expeDir
#cd $expeDir
#for similarityType in cosine; do 
	#dir="${similarityType}"
	#mkdir -p $dir
	#cd $dir

	#computeMae=false
	#computeRmse=true
	#computeIRStats=false
	#doSybilAttack=false
	#for trainingPercentage in 0.9; do
		##for k in 1 2 3 4 5 7 10 12 15 20 25 30 35 40 45 50 75 100; do
		#for k in 10; do
			#SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)
			#java $COMMAND_ARGUMENTS $SIMULATION_PARAMETERS
		#done
	#done

	#cd ../
#done

#cd $outputPath

datasetPath=$baseDir/datasets/movieLens/ml-100k/ratings.csv
#datasetPath=$baseDir/datasets/jester/jester-data-1.csv
#datasetPath=$baseDir/datasets/movietweetings_recsys_challenge_2014/training_test_eval_concat_cleaned.csv
computeMae=false
computeRmse=false
computeIRStats=false
doSybilAttack=true
k=10
nbSybils=1
adaptiveSybils=false
sybilsNbRecoPerRound=1500
similarityType=cosine
nbTargets=1
#nbTargets=943 # all ml100k users
#nbTargets=24983 # all jester users
#nbTargets=24924 # all movietweetings users
neighborhoodType=knn
doOnlyPrecomputations=false

expeDir="test/ml100k/non-adaptive/${neighborhoodType}Neighbors/${similarityType}/one_target_at_a_time/"
#expeDir="test/jester/non-adaptive/${neighborhoodType}Neighbors/${similarityType}/one_target_at_a_time/"
#expeDir="test/movietweetings/non-adaptive/${neighborhoodType}Neighbors/${similarityType}/one_target_at_a_time/"
mkdir -p $expeDir
cd $expeDir

#for targetID in $(seq 1 943); do # ml100k
#for targetID in $(seq 1 24983); do # Jester
#for targetID in $(seq 1 100); do
for targetID in 1; do
	#for percentAuxiliaryItems in 0.1 0.2 0.3 0.5 0.7 0.8 0.9; do
	for percentAuxiliaryItems in 0.8; do
		currentExpe="percentAuxItems_${percentAuxiliaryItems}"
		mkdir -p $currentExpe
		cd $currentExpe

		SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)
		java $COMMAND_ARGUMENTS $SIMULATION_PARAMETERS

		cd ../
	done
done

#cd $outputPath

#similarityType=cosine-mahout
#expeDir="non-adaptiveSybils/${similarityType}"
#mkdir -p $expeDir
#cd $expeDir

#dir="one_target_at_a_time"
#mkdir -p $dir
#cd $dir

#nbTargets=1
#for targetID in $(seq 0 942); do
	#seed=$(($seed + 1))
	#echo "$seed"
	#for percentAuxiliaryItems in 0.1 0.2 0.3 0.5 0.7 0.8 0.9 1.0; do
		#currentExpe="k${k}_targets${nbTargets}_itemsPercent${percentAuxiliaryItems}_sybils${nbSybils}_reco${sybilsNbRecoPerRound}_adaptive"
		#mkdir -p $currentExpe
		#cd $currentExpe

		#SIMULATION_PARAMETERS=$($BASEDIR/scripts/generateParametersString.sh)
		#java $COMMAND_ARGUMENTS $SIMULATION_PARAMETERS
		#$baseDir/scripts/computeNbActualTargets.sh $(pwd)

		#cd ../
	#done
#done


cd $prevWD
