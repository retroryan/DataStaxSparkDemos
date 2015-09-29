package simpleStreaming;

import com.datastax.bdp.spark.DseSparkConfHelper;
import com.datastax.spark.connector.cql.CassandraConnector;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;

/**
*  When you submit a Spark Job using dse spark-submit it automatically sets the Spark Master URL and the Spark Cassandra Connection URL.
*  The Spark Conf then just needs to set the app name.
**/
public interface SparkConfSetup {

    static SparkConf getSparkConf() {
        SparkConf sparkConf = DseSparkConfHelper.enrichSparkConf(new SparkConf()
                .setAppName("SimpleSpark"));

        String contextDebugStr = sparkConf.toDebugString();
        System.out.println("contextDebugStr = " + contextDebugStr);

        return sparkConf;
    }

    static JavaSparkContext getJavaSparkContext() {
        SparkContext sparkContext = new SparkContext(getSparkConf());
        return new JavaSparkContext(sparkContext);
    }

    static CassandraConnector getCassandraConnector() {
        return CassandraConnector.apply((getSparkConf()));
    }
}
