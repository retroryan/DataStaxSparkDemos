package simpleSpark;

import com.datastax.spark.connector.cql.CassandraConnector;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;

/**
*  When you submit a Spark Job using dse spark-submit it automatically sets the Spark Master URL and the Spark Cassandra Connection URL.
*  The Spark Conf then just needs to set the app name.
**/
public interface SparkConfSetup {

    static public SparkConf getSparkConf() {
        return new SparkConf()
                //.setMaster("127.0.0.1:7077")
                .setAppName("SimpleSpark");
    }

    static public JavaSparkContext getJavaSparkContext() {
        SparkContext sparkContext = new SparkContext(getSparkConf());
        return new JavaSparkContext(sparkContext);
    }

    static public CassandraConnector getCassandraConnector() {
        return CassandraConnector.apply((getSparkConf()));
    }
}
