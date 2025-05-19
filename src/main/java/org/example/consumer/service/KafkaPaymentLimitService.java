package org.example.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class KafkaPaymentLimitService {

    private static final String BOOTSTRAP_SERVERS = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
    private static final String TOPIC = "payment_limit";
    private static final String GROUP_ID = "payment-limit-consumer-group11";
    private static final int BATCH_SIZE = 10;

    private final BatchProcessorService batchProcessor;
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    public KafkaPaymentLimitService(BatchProcessorService batchProcessor) {
        this.batchProcessor = batchProcessor;
    }

    @PostConstruct
    public void start() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(BATCH_SIZE));
        properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1024");
        properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "100");

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(TOPIC));

        executorService = Executors.newFixedThreadPool(2);
        executorService.submit(this::pollMessages);
    }

    @Async("kafkaExecutor")
    public void pollMessages() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                if (!records.isEmpty()) {
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    
                    for (ConsumerRecord<String, String> record : records) {
                        futures.add(processRecordAsync(record));
                    }
                    
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
            }
        } catch (Exception e) {
            // Handle error
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    @Async("kafkaExecutor")
    private CompletableFuture<Void> processRecordAsync(ConsumerRecord<String, String> record) {
        return CompletableFuture.runAsync(() -> {
            try {
                String jsonValue = record.value();
                JSONObject jsonObject = new JSONObject(jsonValue);
                batchProcessor.queuePaymentLimitMessage(jsonObject);
            } catch (Exception e) {
                // Handle JSON parsing error
            }
        });
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        batchProcessor.shutdown();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (consumer != null) {
            consumer.close();
        }
    }
}