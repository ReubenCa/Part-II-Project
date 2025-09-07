#mvn clean
set -e
mvn -Dmaven.test.failure.ignore=true -DskipTests package  > maven.log
java -ea -jar pipeline/target/pipeline-1.0-SNAPSHOT-jar-with-dependencies.jar  --runtime ../runtime --nets ../runtime/build/nets.json -o ../runtime/build/program.o --debugnets "../runtime/build/debugnets.json" --debug -t 15 --RECORD_WORK_PER_THREAD --SmallOptimise < exampleinput.ml
echo "RUNNING PROGRAM"
../runtime/build/program.o
echo "\n"