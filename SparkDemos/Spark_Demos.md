#Intro to Spark and Cassandra with the DataStax Platform

## Running a Spark Project against the Spark Master included in DataStax Enterprise

In the next series of demos we will be running the Spark Jobs with the Spark Master that is started with DataStax Enterprise (DSE).  Be sure you have started DSE with Spark Enabled as specified in the main README in the root of the project directory.  That is you have at least one instance of DSE is running as an analytics node.

* Throughout all the demos you can view information on the Spark Master and currently running jobs at [the Spark Master URL](http://localhost:7080/)

* When you submit a Spark Job using dse spark-submit it automatically sets the Spark Master URL and the Spark Cassandra Connection URL.  The Spark Conf then just needs to set the app name.

## Demo 1 - Run a basic Spark Job against the DSE Spark Master

* Build the Simple Spark Project in maven using:
  `mvn package`
* Run the Spark Project by submitting the job to the DSE Spark Master (modify the command to point to where you installed DSE):

  `dse-4.7.3/bin/dse spark-submit --class simpleSpark.BasicSparkDemo ./target/BasicSparkDemo-0.1.jar`

* [See these docs for details about dse spark-submit](http://docs.datastax.com/en/datastax_enterprise/4.6/datastax_enterprise/spark/sparkStart.html)

* Verify the program ran by going to the Spark Master Web Page and finding the stdout of the Spark Job.
  * Standard out will actually be in the log file of the Spark Worker and not in the terminal window where the job ran.
  * The same log file can be found on the server under the /var/lib/spark/work/app ...
  * The program might terminate with an ERROR - that doesn't mean the program didn't run.

## Demo 2 - Connecting to Cassandra from Spark and Writing tables from a Spark JavaRDD into Cassandra

* `dse-4.7.3/bin/dse spark-submit --class simpleSpark.BasicReadWriteDemo ./target/BasicSparkDemo-0.1.jar`

* The cassandra host used to establish a connection to that cassandra server is automatically set when the program is run with dse spark-submit.
* The most basic way to connect to Cassandra from Spark is to create a session and execute CQL on that session.
* A session can be retrieved from the CassandraConnector by calling openSession.
* When finished with the session be sure to close it using:

```
finally {
  if (session != null)
    session.close();
}
```

* In the basicCassandraSession method it creates a session and executes basic CQL.  The CQL creates a keyspace and table and then inserts test data into the table.
* The CQL also create the tables for the saveRDDtoCassandra demos.  (This creates a secondary index which is not recommended.  It is only used for demonstration purposes.  Proper data model would be recommended instead.)

* Verify the tables are created and the data inserted by going to cqlsh and verifying the data.

The second part of this demo shows how to write a list of People in the method saveRDDtoCassandra into Cassandra.

* The saveRDDtoCassandra creates a basic people RDD and then saves that RDD to Cassandra.
* The static import `import static com.datastax.spark.connector.japi.CassandraJavaUtil.*;` brings into scope most of the methods that are needed for working with the Spark Cassandra Connector in Java
* The most import method is `javaFunctions` which converts a JavaRDD to a RDDJavaFunctions.  That class is a Java API Wrapper around the standard RDD functions and adds functionality to interact with Cassandra.
* From the `RDDJavaFunctions` instance that is returned a call is made to `writeBuilder` that defines the mapping of the RDD into Cassandra.  For example the following call:
  * `writerBuilder("test", "people", mapToRow(Person.class))`
  * Maps the write into the keyspace test, table people.  It then uses a Java Bean Mapper to map the Person Class to the proper columns in the people table.
* Finally call saveToCassandra to persist all of the entries in the RDD to Cassandra.

The third part of the demo reads the tables from Cassandra into a Spark JavaRDD

*  Using the `sparkContextJavaFunctions` it loads a Cassandra table into an Java RDD

  `cassandraTable("test", "people")`

*  It then maps each Cassandra row that is returned as a string and print out the rows.

*  It then uses the `mapRowTo` parameter to map each row to a Person automatically:

  `cassandraTable("test", "people", mapRowTo(Person.class))`

*  Finally it adds a filter to the rows.  This is what is called a "push down predicate" because the filter is executed on the server side before it is returned to Cassandra.  This is much more efficient then loaded all of the data into Spark and then filtering it.

* Another example would be the filter  `.where("name=?", "Anna")` after the cassandraTable method.  

* Use a projection to select only certain columns from the table `select("id")`

* It then prints out the ID of each person returned.


## Demo 3 - Word Count Demo - Counting the words in a novel

The goal of this exercise is to get familiar with the more advanced functionality of Spark by reading a text file and storing a count of the words in Cassandra.  An important part of this is learning to use Pair RDD's.

Additional Functionality is provided on RDD's that are key / value pairs.  These RDDs are called pair RDDs.  They provide additional APIs that are performed on each key in the RDD.  Some common examples are reduceByKey which performs a reduce function that operates on each key.  Or there is a join operation which joins two RDD's by key.  

Pair RDD's are defined using a Tuple of 2.  Tuples are defined as "a finite ordered list of elements".  They can be thought of as a generic container of data. In the case of Pair RDD's the first element is the key and the second element is the value.  

In Java a Pair RDD can be created using the method `mapToPair`.  It operates on each element in an RDD and returns a new element that is a Tuple2.   The method 'mapToPair' then expects a function that takes a single element and returns a pair of key and value.

* Copy the data directory in the parent directory into the /tmp directory on BOTH the Spark Driver and the Spark Master where DSE is running.  The program will try and read the text files from the /tmp/data directory.

* Run the program using `dse-4.7.3/bin/dse spark-submit --class simpleSpark.SparkWordCount ./target/BasicSparkDemo-0.1.jar`

* The method sparkWordCount iterates through each of the files in the constant  `DATA_FILES` reads the file into a Spark RDD
  * `javaSparkContext.textFile(DATA_FILE_DIR + fileName);`

* It then splits each string into a list of words.  It uses flatMap and then splits each line with `nextLine.split("\\s+")`

* It converts each word in the RDD to a Pair RDD of (Word, 1) using `mapToPair`.  The second value is what will be used to count each word in the list.

* It uses `reduceByKey` to count all words.

* It then maps the results of the list to the `WordCountFileName` to make it easy to save out to Cassandra.

* It then saves the count of words to Cassandra

* For a bonus exercise filter out stop words to improve the accuracy of the count.

## Further Exercises for Word Count Demo - Analyzing the results

* Read the results back out of the database for all datasets into WordCountFileName.

* Map the results to a Pair RDD with the count as the key

* Find the top word counts out of the RDD.


## Further Documentation

[Spark Cassandra Connector Java API Documentation](https://github.com/datastax/spark-cassandra-connector/blob/master/doc/7_java_api.md)

[Accessing cassandra from spark in java](http://www.datastax.com/dev/blog/accessing-cassandra-from-spark-in-java)
