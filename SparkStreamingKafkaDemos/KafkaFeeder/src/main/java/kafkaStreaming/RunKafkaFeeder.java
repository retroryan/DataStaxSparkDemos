package kafkaStreaming;


import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class RunKafkaFeeder {


    public static void main(String[] args) throws InterruptedException {

        if (args.length < 1) {
            System.out.println("need to pass kafka host at command line, i.e.  localhost:9092");
            System.exit(-1);
        }

        String kafkaHost = args[0];
        System.out.println("starting on kafkaHost = " + kafkaHost);

        startKafkaServer(kafkaHost);
    }

    private static void startKafkaServer(String kafkaHost) throws InterruptedException {

        Properties props = new Properties();
        props.put("metadata.broker.list", kafkaHost);
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        ProducerConfig config = new ProducerConfig(props);
        Producer<String, String> producer = new Producer<String, String>(config);

        while (true) {
            String nxtMessage = StringUtils.join(nextEventList(), ' ');
            System.out.println("sending nextEventList = " + nxtMessage);
            producer.send(new KeyedMessage<String, String>(StreamingProperties.kafkaTopic, nxtMessage));
            Thread.sleep(500);
        }
    }

    public static List<String> EVENT_NAMES = Arrays.asList("thyrotome", "radioactivated", "toreutics", "metrological",
            "adelina", "architecturally", "unwontedly", "histolytic", "clank", "unplagiarised",
            "inconsecutive", "scammony", "pelargonium", "preaortic", "goalmouth", "adena",
            "murphy", "vaunty", "confetto", "smiter", "chiasmatype", "fifo", "lamont", "acnode",
            "mutating", "unconstrainable", "donatism", "discept", "expressions", "benevolent");

    private static Random rand = new Random();

    private static Integer MAX_STREAM_SIZE = 5;

    private static List<String> nextEventList() {
        List<String> nextStream = new ArrayList<>(MAX_STREAM_SIZE);

        for (int indx = 0; indx < MAX_STREAM_SIZE; indx++) {
            int randomNum = rand.nextInt(EVENT_NAMES.size());
            String event = EVENT_NAMES.get(randomNum);
            nextStream.add(event);
        }

        return nextStream;
    }


}
