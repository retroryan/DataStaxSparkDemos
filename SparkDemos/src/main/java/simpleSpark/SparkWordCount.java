package simpleSpark;

import com.datastax.driver.core.Session;
import com.datastax.spark.connector.cql.CassandraConnector;
import com.datastax.spark.connector.japi.rdd.CassandraJavaPairRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.*;

public class SparkWordCount {

    public static String DATA_FILE_DIR = "file:///tmp/data/";
    public static String STOP_WORDS = "stopWords.txt";
    public static List<String> DATA_FILES = Arrays.asList("LesMiserables.txt", "TheAdventuresOfSherlockHolmes.txt");

    public static void main(String[] args) {

        JavaSparkContext javaSparkContext = SparkConfSetup.getJavaSparkContext();
        CassandraConnector connector = SparkConfSetup.getCassandraConnector();

        setupCassandraTables(connector);
        sparkWordCount(javaSparkContext);

        javaSparkContext.stop();

    }


    private static void setupCassandraTables(CassandraConnector connector) {
        try (Session session = connector.openSession()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS demo WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }");
            session.execute("CREATE TABLE IF NOT EXISTS demo.wordcount (filename TEXT, word TEXT, count INT, PRIMARY KEY (filename, word, count))");
            session.execute("TRUNCATE demo.wordcount");
        }
    }

    private static void sparkWordCount(JavaSparkContext javaSparkContext) {

        DATA_FILES.forEach(fileName -> {
            JavaRDD<String> stringJavaRDD = javaSparkContext.textFile(DATA_FILE_DIR + fileName);
            System.out.println("processing fileName = " + fileName + " with " + stringJavaRDD.count() + " words.");

            JavaPairRDD<String, Integer> wordCountPairRDD =
                    stringJavaRDD.flatMap(nextLine -> Arrays.asList(nextLine.split("\\s+")))
                    .mapToPair(word -> new Tuple2<>(word.toLowerCase(), 1))
                    .reduceByKey((keyCount, wordCount) -> keyCount + wordCount);


            wordCountPairRDD
                    .map(wordCount -> wordCount.swap())
                    .sortBy(wordCount -> wordCount._1(), false, 1)
                    .take(20);

            JavaRDD<WordCountFileName> wordCountRDD = wordCountPairRDD.map(wordCountPair -> new WordCountFileName(fileName, wordCountPair._1(), wordCountPair._2()));

            javaFunctions(wordCountRDD)
                    .writerBuilder("demo", "wordcount", mapToRow(WordCountFileName.class))
                    .saveToCassandra();
        });
    }


    private static void analyzeWordCount(JavaSparkContext javaSparkContext) {

    }

    public static class WordCountFileName implements Serializable {
        private String filename;
        private String word;
        private Integer count;

        public WordCountFileName(String filename, String word, Integer count) {
            this.filename = filename;
            this.word = word;
            this.count = count;
        }

        public String getFilename() {
            return filename;
        }

        public String getWord() {
            return word;
        }

        public Integer getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "WordCountFileName{" +
                    "filename='" + filename + '\'' +
                    ", word='" + word + '\'' +
                    ", count=" + count +
                    '}';
        }
    }

}
