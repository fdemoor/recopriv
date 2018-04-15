#! /bin/bash
# Compile all java source files in $BASEDIR/src/ and put the binaries in $BASEDIR/bin/.
# Compilation uses the Java 7 specification and the CLASSPATH is made of the 
# current directory, $BASEDIR/bin/, and $BASEDIR/lib/.

# Find where this script is stored and set BASEDIR
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export BASEDIR="$(dirname $DIR)"

# Find all Java source files in $BASEDIR/src/
SRC=`find $BASEDIR/src/ -type f -print | grep -E '\.java$'`
#SRC=`find $BASEDIR/src/ -type f -print | grep -E '\.java$' | grep -vE 'SimulationLauncher' | grep -vE 'aggregate' | grep -vE 'slicer' | grep -vE 'items'`

# Log in $BASEDIR/scripts/ the list of all Java source files found in $BASEDIR/src/
echo $SRC > $BASEDIR/scripts/source_files_list.txt

# Compiles all Java source files and put the binaries in $BASEDIR/bin/
mkdir -p $BASEDIR/bin/
javac $SRC -d $BASEDIR/bin/ -cp "./:$BASEDIR/bin/:$BASEDIR/lib/*" -source 1.7 -Xlint

echo "Compilation completed"
