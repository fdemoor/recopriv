#!/bin/bash

# Compile Mahout core (using Maven) in core/target/
mvn -DskipTests=true clean package -pl core
