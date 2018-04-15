#!/bin/bash

###############################################################################
#                  PLOTTING RESULTS SCRIPT, USING MATPLOTLIB                  #
###############################################################################


###############################################################################
# Cheching if 'plot.py' file is present                                       #
BASEDIR=$(dirname "$(pwd)")

if [[ ! -f $BASEDIR/scripts/plot.py ]]; then
 echo "plot.py is missing";
 exit
fi
###############################################################################


###############################################################################
# Path of the dir, where $PLOTS dirs are located                              #
DIRTARGET=/ml100k/speAttack80
###############################################################################


###############################################################################
# Additionnal path to data after $PLOTS dirs (optionnal)                      #
ADDITIONNAL_PATH=
###############################################################################


###############################################################################
# What will be in ordinate, expected values are:                              #
# expectedNeighborhoods ; yield ; accuracy ; infiltration ; sybN ; TiN ; PSC  #
# AxY ; RMSE                                                                  #
# Several can be put, separetad by a coma with no space: A,B                  #
# If so, plotting code must be adapted according to subplots number           #
ORDINATE=expectedNeighborhoods,sybN,TiN,accuracy
CODE=220
###############################################################################


###############################################################################
# What will be in abscissa, expected values are:                              #
# auxPer ; nbExtra ; iPerNb ; k ; bwFrac                                      #
# Only one value                                                              #
ABSCISSA=auxPer
###############################################################################


###############################################################################
# What will be plot                                                           #
# Several can be put, separetad by a coma with no space: A,B                  #
# Labels can be empty, but there must be as many comas as in $PLOTS           #
PLOTS=specificAttack1682_3_100,specificAttack1682_worstRated_100
LABELS="random,worstRated"
###############################################################################


###############################################################################
# Generating string argument containing parameters and calling python script  #
PARAM="$BASEDIR/output$DIRTARGET $ABSCISSA $ORDINATE $CODE
$PLOTS $LABELS $ADDITIONNAL_PATH"
echo "Plot started"
python3 plot.py $PARAM
echo "Plot completed"
###############################################################################
