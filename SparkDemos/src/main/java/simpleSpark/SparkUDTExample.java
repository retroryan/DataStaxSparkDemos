package simpleSpark;

import com.datastax.driver.core.Session;
import com.datastax.spark.connector.cql.CassandraConnector;
import com.datastax.spark.connector.japi.*;
import com.datastax.spark.connector.japi.rdd.CassandraTableScanJavaRDD;
import com.google.common.base.Objects;
import org.apache.hadoop.util.StringUtils;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.*;

public class SparkUDTExample {

    public static void main(String[] args) {

        JavaSparkContext javaSparkContext = SparkConfSetup.getJavaSparkContext();

        CassandraConnector connector = SparkConfSetup.getCassandraConnector();

        saveRDDtoCassandra(javaSparkContext);

        javaSparkContext.stop();

    }

    private static void saveRDDtoCassandra(JavaSparkContext javaSparkContext) {

        ITEM item = new ITEM("1","another value","spanish");
        FULLCONTENT fullcontent = new FULLCONTENT("1", item);

        ITEM item2 = new ITEM("2","another value","spanish");
        FULLCONTENT fullcontent2 = new FULLCONTENT("2", item2);


        // here we are going to save some data to Cassandra...
        List<FULLCONTENT> fullcontentList = Arrays.asList(fullcontent, fullcontent2);

        JavaRDD<FULLCONTENT> fullcontentJavaRDD = javaSparkContext.parallelize(fullcontentList);
        javaFunctions(fullcontentJavaRDD).writerBuilder("test", "fullcontent", mapToRow(FULLCONTENT.class)).saveToCassandra();

    }

   // case class ITEM(cms_id:String, tenant:String, locale:String)
   // case class FULLCONTENT(contentid:String, item:UDTValue)

    public static class ITEM implements Serializable {
        private String cms_id;
        private String tenant;
        private String locale;

        public ITEM(String cms_id, String tenant, String locale) {
            this.cms_id = cms_id;
            this.tenant = tenant;
            this.locale = locale;
        }

        public String getCms_id() {
            return cms_id;
        }

        public void setCms_id(String cms_id) {
            this.cms_id = cms_id;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }
    }

    public static class FULLCONTENT implements Serializable {
        private String contentid;
        private ITEM item;

        public FULLCONTENT(String contentid, ITEM item) {
            this.contentid = contentid;
            this.item = item;
        }

        public String getContentid() {
            return contentid;
        }

        public void setContentid(String contentid) {
            this.contentid = contentid;
        }

        public ITEM getItem() {
            return item;
        }

        public void setItem(ITEM item) {
            this.item = item;
        }
    }
}
