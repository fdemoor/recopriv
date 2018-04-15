#!/bin/bash 

# Find where this script is stored and set BASEDIR
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export BASEDIR="$(dirname $DIR)"

export CLASSPATH=".:$BASEDIR/bin/:$BASEDIR/lib/*"


mkdir -p $BASEDIR/output/local/ml100k/distrib/

for i in 7621 13698 25180 50351; do
	$BASEDIR/scripts/computeSimilarityDistribution.sh $BASEDIR/scripts/log4j.properties $BASEDIR/datasets/movieLens/ml-100k/sparser_variants/ml1-ratings${i}.csv ml100k-${i} $BASEDIR/output/local/ml100k/distrib/
done
