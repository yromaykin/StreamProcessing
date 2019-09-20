Step 1:
# Events generator
python3 botgen.py -b 1 -u 1000 -n 100 -d 10 -f data/data.json

Step 2.a:
# Kafka
cd ~/streaming/kafka-docker
docker-compose up

# connect to kafka-docker_kafka container id = b4b9a476a7f9
docker exec -it b4b9a476a7f9 /bin/bash

# $KAFKA_HOME = /opt/kafka
cd $KAFKA_HOME/bin

# topics
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper docker.for.mac.localhost:2181 --create --topic connect-test --partitions 4 --replication-factor 1

$KAFKA_HOME/bin/kafka-topics.sh --zookeeper docker.for.mac.localhost:2181 --list

Step 3.a:
# Kafka Connect
$KAFKA_HOME/bin/connect-standalone.sh /capstone/connect/connect-standalone.properties /capstone/connect/connect-file-source.properties

Step 2.b:
# Kafka with UI http://localhost:3030
cd ~/streaming/kafka-docker-landoop
docker-compose up kafka-cluster
# docker-compose -f docker-compose-with-ignite.yml up

# connect to kafka-docker container id = bfbe402feb3e
docker exec -it bfbe402feb3e bash
# docker run --rm -it --net=host landoop/fast-data-dev bash

# topics
kafka-topics --create --topic ad-events --partitions 3 --replication-factor 1 --zookeeper 127.0.0.1:2181

kafka-topics --list --zookeeper 127.0.0.1:2181




Step 3.b:
# Kafka Connect
connect-standalone /capstone/connect/connect-standalone.properties /capstone/connect/connect-file-source.properties
connect-standalone /capstone/scripts/connect-standalone.properties /capstone/scripts/connect-file-source.properties

# transforms=replaceRegex
# transforms.replaceRegex.type=org.apache.kafka.connect.transforms.RegexRouter
# transforms.replaceRegex.regex="^\\[?(\\{.*\\})[\\,\\]]?$"
# transforms.replaceRegex.replacement=$1


Step 4:
# Spark





