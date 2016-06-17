package simpleSpark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.hive.HiveContext;

import java.util.Arrays;
import java.util.List;

/**
 * dse spark-submit --class simpleSpark.SparkSQLDemo ./target/BasicSparkDemo-0.1.jar
 */
public class SparkSQLDemo {


    public static void main(String[] args) {

        // create a new configuration
        SparkConf conf = new SparkConf()
                .setAppName("My application");
// create a Spark context
        JavaSparkContext javaSparkContext = new JavaSparkContext(conf);

        // sc is an existing JavaSparkContext.
        HiveContext sqlContext = new org.apache.spark.sql.hive.HiveContext(javaSparkContext.sc());

        DataFrame stock_summary = sqlContext.sql("SELECT * FROM stocks.stock_summary");
        stock_summary.registerTempTable("stock_summary");
        DataFrame closes = sqlContext.sql("SELECT dateoffset, close FROM stock_summary WHERE symbol == 'GOOG' ");
        closes.printSchema();
        closes.show();

        javaSparkContext.stop();
    }
}
