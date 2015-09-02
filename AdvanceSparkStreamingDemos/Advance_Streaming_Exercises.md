#Demo of Advance Spark Streaming and Cassandra with DataStax Enterprise Platform

These demos demonstrate Advance Spark Streaming and Cassandra with DataStax Enterprise Platform.

##Demo -  Listen to a socket to receive a basic text stream.  Perform a windowing word count on the text socket.

* To build and start the word socket server run the following.  Be sure to set the IP address to your local or server IP address.
```
mvn package
java -jar target/AdvanceSparkStreamingDemos-0.1-jar-with-dependencies.jar 127.0.0.1 9797`
```

* This starts the word socket server defined in `advanceStreaming.RunFeeder`

* To submit the Spark Job run the following and be sure to set the IP address to your local or server IP address.
```
mvn install
dse-4.7.3/bin/dse spark-submit --class advanceStreaming.RunReceiver ./target/AdvanceSparkStreamingDemos-0.1.jar 127.0.0.1 9797
```

* The code for the streaming demo is found in `advanceStreaming.RunReceiver`