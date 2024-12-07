#!/bin/sh
set -e 


# (
#   cd "$(dirname "$0")"
#   mvn -B package -Ddir=/home/bryce/Code/gigs/task
# )


exec java -jar /home/bryce/Code/gigs/task/target/task-1.0-SNAPSHOT.jar "$@"
