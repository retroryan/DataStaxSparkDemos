#Demo of Advance Spark Streaming and Cassandra with DataStax Enterprise Platform

These demos demonstrate Spark Streaming and Cassandra with DataStax Enterprise Platform.

##Demo -  Listen to a socket to receive a basic text stream.  Perform a windowing word count on the text socket.

* To build and start the word socket server run the following.  Be sure to set the IP address to your local or server IP address.
```
mvn package
java -jar target/AdvanceSparkStreamingDemos-0.1-jar-with-dependencies.jar 127.0.0.1 9797`
```

* This starts the word socket server defined in `simpleStreaming.RunFeeder`

* To submit the Spark Job run the following and be sure to set the IP address to your local or server IP address.
```
mvn install
dse-4.7.3/bin/dse spark-submit --class simpleStreaming.RunReceiver ./target/AdvanceSparkStreamingDemos-0.1.jar 127.0.0.1 9797
```

* The code for the streaming demo is found in `simpleStreaming.RunReceiver`

* A Spark Stream is built using a Streaming Context. The Streaming Context will divide the stream of data into micro-batches based on the batch Duration.  

* The Streaming Sample creates a Streaming Context with a batch duration of 1 second:
```
new StreamingContext(SparkConfSetup.getSparkConf(), batchDuration);
```

* A receiver is then created from the streaming context.  The receiver can be any input stream that receives data over the network.  Each time it receives data from the network it stores the data into to Spark's memory.  These single items will be aggregated together into data blocks before being pushed into the next micro-batch for processing.

* Next it creates a socket text stream that listens for a stream of words.  The storage level specifies where and how the received data gets stored before being processed:

```
javaStreamingContext.socketTextStream(
                "127.0.0.1", Integer.parseInt("9999"), StorageLevels.MEMORY_AND_DISK_SER);
```

* It then creates a checkpoint on the stream that will periodically persist the stream.  The checkpoint should be created as a multiple of the streaming window. In this example the checkpoint is set to every 30 seconds.

```
javaStreamingContext.checkpoint("/tmp/spark");
lineStream.checkpoint(getDurationsSeconds(30));
```

* Similar to the word count demo in the basic Spark Demos this streaming demo counts the words of each stream of words as they are received.  

* It writes the word counts to Cassandra

##Additional Exercises - Windowing Operations

* Windowed operation operate on sliding window of data, for example if the batch is recieving data every 5 seconds then the following image would show working on the last 15 seconds of data:

![](https://spark.apache.org/docs/latest/img/streaming-dstream-window.png)

* Instead of keeping a count of every batch of words received we could create a word count of the words received in the past 30 seconds every 10 seconds using `reduceByKeyAndWindow`

* How would you data model this in Cassandra?

* Save the word count to Cassandra.
