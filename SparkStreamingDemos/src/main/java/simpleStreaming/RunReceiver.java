package simpleStreaming;

import com.datastax.driver.core.Session;
import com.datastax.spark.connector.cql.CassandraConnector;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.deploy.SparkHadoopUtil$;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.api.java.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import scala.Tuple2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import org.apache.spark.streaming.api.java.JavaStreamingContext;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.mapToRow;
import static com.datastax.spark.connector.japi.CassandraStreamingJavaUtil.javaFunctions;


public class RunReceiver {

    public static Duration getDurationsSeconds(int seconds) {
        return new Duration(seconds * 1000);
    }

    static String CHECKPOINT_DIR = "/stream_demo";

    public static JavaStreamingContext getJavaStreamingContext(Duration batchDuration, String hostname, int port) {

        SparkConf sparkConf = SparkConfSetup.getSparkConf();

        JavaStreamingContextFactory contextFactory = () -> {
            System.out.println("Setting up a new streaming context with checkpoint");
            StreamingContext streamingContext = new StreamingContext(sparkConf, batchDuration);
            JavaStreamingContext jssc = new JavaStreamingContext(streamingContext);
            jssc.checkpoint(CHECKPOINT_DIR);

            JavaReceiverInputDStream<String> lineStream = jssc.socketTextStream(
                    hostname, port, StorageLevels.MEMORY_AND_DISK_SER);

            lineStream.checkpoint(getDurationsSeconds(30));

            basicWordsMapAndSave(lineStream);

            return jssc;
        };


        JavaStreamingContext orCreate = JavaStreamingContext.getOrCreate(CHECKPOINT_DIR, contextFactory);
        System.out.println("orCreate = " + orCreate);

        return orCreate;
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("need to set hostname and port in pom.xml or at the command line");
            System.exit(-1);
        }
        String hostname = args[0];
        String tmpPort = args[1];
        int port = Integer.parseInt(tmpPort);

        //CassandraConnector connector = SparkConfSetup.getCassandraConnector();
        //setupCassandraTables(connector);

        System.out.println("Setting up java streaming context");
        JavaStreamingContext javaStreamingContext = getJavaStreamingContext(getDurationsSeconds(1), hostname, port);

        javaStreamingContext.start();
        javaStreamingContext.awaitTermination();
    }


    private static void basicWordsMapAndSave(JavaReceiverInputDStream<String> lines) {
        JavaPairDStream<String, Integer> wordMap = lines.flatMap(word -> Arrays.asList(word.split("\\s+")))
                .mapToPair(word -> new Tuple2<>(word.toLowerCase(), 1))
                .reduceByKeyAndWindow((keyCount, wordCount) -> keyCount + wordCount, getDurationsSeconds(30), getDurationsSeconds(10));  // Reduce last 30 seconds of data, every 10 seconds


        JavaDStream<WordCount> wordCountStream = wordMap.map(wordCountPair -> new WordCount(wordCountPair._1(), wordCountPair._2(), new DateTime()));

        javaFunctions(wordCountStream)
                .writerBuilder("streamdemo", "wordcount", mapToRow(WordCount.class))
                .saveToCassandra();

    }

    private static void setupCassandraTables(CassandraConnector connector) {
        try (Session session = connector.openSession()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS streamdemo WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }");
            session.execute("DROP TABLE IF EXISTS streamdemo.wordcount;");
            session.execute("CREATE TABLE IF NOT EXISTS streamdemo.wordcount (timewindow TEXT, word TEXT, count INT, PRIMARY KEY (timewindow, word, count))");

            session.execute("DROP TABLE IF EXISTS streamdemo.word_analytics;");
            session.execute("CREATE TABLE IF NOT EXISTS streamdemo.word_analytics (series text, " +
                    "  timewindow timestamp, " +
                    "  quantities map<text, int>, " +
                    "  PRIMARY KEY ((series), timewindow) " +
                    ") WITH CLUSTERING ORDER BY (timewindow DESC)");
        }
    }

    //Format the date as the "Day of the Year" "hour of the day" and "minute of the hour" and "second of the minute" to bucket data for the current minute
    private static DateTimeFormatter fmt = DateTimeFormat.forPattern("DHms");

    public static class WordCount implements Serializable {
        private String word;
        private Integer count;
        private DateTime timewindow;

        public WordCount(String word, Integer count, DateTime timewindow) {
            this.word = word;
            this.count = count;
            this.timewindow = timewindow;
        }

        public String getWord() {
            return word;
        }

        public Integer getCount() {
            return count;
        }

        public String getTimewindow() {
            return timewindow.toString(fmt);
        }

        @Override
        public String toString() {
            return "WordCount{" +
                    "word='" + word + '\'' +
                    ", count=" + count +
                    ", timewindow=" + timewindow +
                    '}';
        }
    }


    public static class WordCountAnalysis implements Serializable {
        private String series;
        private DateTime timewindow;
        Map<String, Integer> quantities;

        public WordCountAnalysis(String series, DateTime timewindow, Map<String, Integer> quantities) {
            this.series = series;
            this.timewindow = timewindow;
            this.quantities = quantities;
        }

        public String getSeries() {
            return series;
        }

        public DateTime getTimewindow() {
            return timewindow;
        }

        public Map<String, Integer> getQuantities() {
            return quantities;
        }

        @Override
        public String toString() {
            return "WordCountAnalysis{" +
                    "series='" + series + '\'' +
                    ", timewindow=" + timewindow +
                    ", quantities=" + quantities +
                    '}';
        }
    }

}
