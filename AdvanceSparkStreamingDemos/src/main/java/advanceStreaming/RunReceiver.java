package advanceStreaming;

import com.datastax.driver.core.Session;
import com.datastax.spark.connector.cql.CassandraConnector;
import com.datastax.spark.connector.japi.DStreamJavaFunctions;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.joda.time.DateTime;
import scala.Tuple2;

import java.io.Serializable;
import java.util.List;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.mapToRow;
import static com.datastax.spark.connector.japi.CassandraStreamingJavaUtil.javaFunctions;

import com.google.common.base.Optional;

public class RunReceiver {

    public static Duration getDurationsSeconds(int seconds) {
        return new Duration(seconds * 1000);
    }

    public static JavaStreamingContext getJavaStreamingContext(Duration batchDuration) {
        StreamingContext streamingContext = new StreamingContext(SparkConfSetup.getSparkConf(), batchDuration);
        return new JavaStreamingContext(streamingContext);
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("need to set hostname and port in pom.xml or at the command line");
            System.exit(-1);
        }
        String hostname = args[0];
        String tmpPort = args[1];
        int port = Integer.parseInt(tmpPort);

        CassandraConnector connector = SparkConfSetup.getCassandraConnector();
        setupCassandraTables(connector);


        JavaStreamingContext javaStreamingContext = getJavaStreamingContext(getDurationsSeconds(1));
        JavaReceiverInputDStream<String> lineStream = javaStreamingContext.socketTextStream(
                hostname, port, StorageLevels.MEMORY_AND_DISK_SER);

        javaStreamingContext.checkpoint("/tmp/spark");
        lineStream.checkpoint(getDurationsSeconds(30));

        basicWordsMapAndSave(lineStream);

        javaStreamingContext.start();
        javaStreamingContext.awaitTermination();
    }


    private static void basicWordsMapAndSave(JavaReceiverInputDStream<String> lines) {

        Function2<List<Integer>, Optional<WordRatingState>, Optional<WordRatingState>> updateFunction =
                (values, state) -> {
                    final WordRatingState wordStateRating = (state.isPresent() ? state.get() : new WordRatingState());

                    values.forEach(rating -> {
                        int newTotal = wordStateRating.getRatingsTotal()  + rating;
                        int numRatings = wordStateRating.getNumRatings() + 1;
                        wordStateRating.setCrntRating(newTotal / numRatings);
                        wordStateRating.setRatingsTotal(newTotal);
                        wordStateRating.setNumRatings(numRatings);
                    });
                    return Optional.of(wordStateRating);
                };

        JavaPairDStream<String, Integer> wordRatings = lines.mapToPair(word -> {
            String[] split = word.split(",");
            if (split.length == 2)
                return new Tuple2<>(split[0], Integer.parseInt(split[1]));
            else
                return new Tuple2<>("",0);
        }).filter(wordPair -> wordPair._1().length() != 0);

        JavaDStream<Word> wordJavaDStream = wordRatings.map(nxtWordRatingsTuple -> {
            String nxtWord = nxtWordRatingsTuple._1();
            Integer rating = nxtWordRatingsTuple._2();
            return new Word(nxtWord, rating, new DateTime());
        });

        DStreamJavaFunctions<Word> wordDStreamJavaFunctions = javaFunctions(wordJavaDStream);
        wordDStreamJavaFunctions.writerBuilder("streamdemo", "wordcount_avg", mapToRow(Word.class)).saveToCassandra();


        JavaPairDStream<String, WordRatingState> stringWordCountRatingJavaPairDStream = wordRatings.updateStateByKey(updateFunction);

        JavaDStream<WordAvgRating> wordCountAvgJavaDStream = stringWordCountRatingJavaPairDStream
                .map(nxtWordRatingsTuple -> {

                    String nxtWord = nxtWordRatingsTuple._1();
                    WordRatingState nxtWordRatings = nxtWordRatingsTuple._2();
                    return new WordAvgRating(nxtWord, nxtWordRatings.getCrntRating());
                });


        //  Doesn't seem possible to have just a single static column?
        //  error:  Missing required columns in RDD data: ratingtime, rating
        //  DStreamJavaFunctions<WordAvgRating> wordAvgRatingDStreamJavaFunctions = javaFunctions(wordCountAvgJavaDStream);
        //  wordAvgRatingDStreamJavaFunctions.writerBuilder("streamdemo", "wordcount_avg", mapToRow(WordAvgRating.class)).saveToCassandra();

        wordRatings.print();
    }

    private static void setupCassandraTables(CassandraConnector connector) {
        try (Session session = connector.openSession()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS streamdemo WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }");
            session.execute("DROP TABLE IF EXISTS streamdemo.wordcount_avg;");

            session.execute("DROP TABLE IF EXISTS streamdemo.wordcount_avg;");
            session.execute("CREATE TABLE IF NOT EXISTS streamdemo.wordcount_avg (word text, " +
                    "  ratingtime timestamp, " +
                    "  rating int, " +
                   // having a static column doesn't work because it is required when writing an RDD
                   // "  avgRating int static, " +
                    "  PRIMARY KEY ((word), ratingtime) " +
                    ") WITH CLUSTERING ORDER BY (ratingtime DESC)");
        }
    }

    public static class Word implements Serializable {
        private String word;
        private Integer rating;
        private DateTime ratingtime;

        public Word(String word, Integer rating, DateTime ratingtime) {
            this.word = word;
            this.rating = rating;
            this.ratingtime = ratingtime;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public DateTime getRatingtime() {
            return ratingtime;
        }

        public void setRatingtime(DateTime ratingtime) {
            this.ratingtime = ratingtime;
        }
    }

    public static class WordAvgRating implements Serializable {
        private String word;
        private Integer avgRating;

        public WordAvgRating(String word, Integer avgRating) {
            this.word = word;
            this.avgRating = avgRating;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public Integer getAvgRating() {
            return avgRating;
        }

        public void setAvgRating(Integer avgRating) {
            this.avgRating = avgRating;
        }
    }


    public static class WordRatingState implements Serializable {
        private Integer crntRating = 0;
        private Integer numRatings = 0;
        private Integer ratingsTotal = 0;

        public WordRatingState() {
        }

        public WordRatingState(Integer crntRating, Integer numRatings, Integer ratingsTotal) {
            this.crntRating = crntRating;
            this.numRatings = numRatings;
            this.ratingsTotal = ratingsTotal;
        }

        public Integer getCrntRating() {
            return crntRating;
        }

        public void setCrntRating(Integer crntRating) {
            this.crntRating = crntRating;
        }

        public Integer getNumRatings() {
            return numRatings;
        }

        public void setNumRatings(Integer numRatings) {
            this.numRatings = numRatings;
        }

        public Integer getRatingsTotal() {
            return ratingsTotal;
        }

        public void setRatingsTotal(Integer ratingsTotal) {
            this.ratingsTotal = ratingsTotal;
        }

        @Override
        public String toString() {
            return "WordCountRating{" +
                    "crntRating=" + crntRating +
                    ", numRatings=" + numRatings +
                    ", ratingsTotal=" + ratingsTotal +
                    '}';
        }
    }

}
