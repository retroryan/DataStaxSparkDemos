#Spark Streaming Exercises

Kafka Setup - Local Machine
==============================

Start Kafka, create the topics and test:

bin/zookeeper-server-start.sh config/zookeeper.properties

bin/kafka-server-start.sh config/server.properties

bin/kafka-create-topic.sh --zookeeper localhost:2181 --replica 1 --partition 1 --topic wordbucket

bin/kafka-list-topic.sh --zookeeper localhost:2181

bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic wordbucket --from-beginning

Kafka Server Setup - Remote Server Setup
===================================

Everything is run in the kafka root directory:

mkdir runlogs

nohup bin/zookeeper-server-start.sh config/zookeeper.properties > runlogs/zookeeper.log 2> runlogs/zookeeper.err < /dev/null &

nohup bin/kafka-server-start.sh config/server.properties > runlogs/kafka.log 2> runlogs/kafka.err < /dev/null &

#kafka 0.8
bin/kafka-create-topic.sh --zookeeper localhost:2181 --replica 1 --partition 1 --topic wordbucket
bin/kafka-list-topic.sh --zookeeper localhost:2181

#kafka 0.8.2
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic wordbucket
bin/kafka-topics.sh --list --zookeeper localhost:2181

bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic wordbucket --from-beginning


Setup and Run DSE
=============================
[If running DSE 4.6 fix DSE so Kafka Spark Streaming works](https://support.datastax.com/hc/en-us/articles/204226489--java-lang-NoSuchMethodException-seen-when-attempting-Spark-streaming-from-Kafka)

Run DSE as an analytics node:
dse/bin/dse cassandra -k

To build and run the Kafka Feeder Example
========================================

* cd into the KafkaFeeder directory and build and run the project in that directory
* Build the jar file -> 'mvn package'
* java -jar target/KafkaFeeder-0.1-jar-with-dependencies.jar localhost:9092
* Make sure you are running spark server and Cassandra node listening on localhost
* Make sure you are running Kafka server on localhost with the topic events pre-provisioned.

To run the Kafka Streaming
=========================================

dse spark-submit --packages org.apache.spark:spark-streaming-kafka_2.10:1.4.1 --class kafkaStreaming.RunKafkaReceiver KafkaStreaming-0.1.jar localhost:2181

dse spark-submit --deploy-mode cluster --supervise --class kafkaStreaming.RunKafkaReceiver target/KafkaStreaming-0.1-jar-with-dependencies.jar localhost:2181

dse spark-class  org.apache.spark.deploy.Client kill spark://172.31.3.141:7077 driver-20150611050849-0003
