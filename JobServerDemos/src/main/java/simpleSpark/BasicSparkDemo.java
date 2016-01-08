package simpleSpark;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import java.util.Arrays;
import java.util.List;

public class BasicSparkDemo {


    public static void main(String[] args) {

        JavaSparkContext javaSparkContext = SparkConfSetup.getJavaSparkContext();

//Create a basic Array List here and convert it to a JavaRDD
        List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        JavaRDD<Integer> listRDD = javaSparkContext.parallelize(intList);

        Integer sum = listRDD.reduce((currentCount, nextElement) -> currentCount + nextElement);


        listRDD.reduce((crntMin, nxt) -> crntMin < nxt ? crntMin : nxt);


        JavaRDD<List<Integer>> mappedRDD = listRDD.map(indx -> Arrays.asList(indx, indx * 2, indx * 3));
        mappedRDD.foreach(indx -> System.out.println("indx = " + indx));


        JavaRDD<Integer> flatMappedRDD = listRDD.
                filter(x -> (x % 2 == 0))
                .flatMap(indx -> Arrays.asList(indx, indx * 2, indx * 3));

        JavaRDD<Integer> distinctRDD = flatMappedRDD
            .distinct();

        distinctRDD.foreach(indx -> System.out.println("indx = " + indx));

        System.out.println("flatMappedRDD = " + distinctRDD.toDebugString());

        javaSparkContext.stop();
    }
}
