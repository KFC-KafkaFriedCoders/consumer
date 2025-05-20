package org.example.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.consumer.config.KafkaConfig;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

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
import java.util.logging.Logger;

public abstract class AbstractKafkaConsumerService {
    
    private static final Logger logger = Logger.getLogger(AbstractKafkaConsumerService.class.getName());
    
    @Autowired
    private KafkaConfig kafkaConfig;
    
    protected final BatchProcessorService batchProcessor;
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    protected AbstractKafkaConsumerService(BatchProcessorService batchProcessor) {
        this.batchProcessor = batchProcessor;
    }
    
    protected abstract String getTopic();
    protected abstract String getGroupId();
    protected abstract int getBatchSize();
    protected abstract int getFetchMinBytes();
    protected abstract int getFetchMaxWaitMs();
    protected abstract int getPollTimeoutMs();
    protected abstract CompletableFuture<Void> processMessage(JSONObject jsonObject);
    
    @PostConstruct
    public void start() {
        Properties properties = createConsumerProperties();
        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(getTopic()));
        
        executorService = Executors.newFixedThreadPool(2);
        executorService.submit(this::pollMessages);
    }
    
    private Properties createConsumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, getGroupId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(getBatchSize()));
        properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, String.valueOf(getFetchMinBytes()));
        properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, String.valueOf(getFetchMaxWaitMs()));
        return properties;
    }
    
    @Async("kafkaExecutor")
    public void pollMessages() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(getPollTimeoutMs()));
                
                if (!records.isEmpty()) {
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    
                    for (ConsumerRecord<String, String> record : records) {
                        futures.add(processRecordAsync(record));
                    }
                    
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
            }
        } catch (Exception e) {
            logger.severe("Error in Kafka consumer polling: " + e.getMessage());
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
                processMessage(jsonObject);
            } catch (Exception e) {
                logger.warning("Failed to process JSON message: " + e.getMessage());
            }
        });
    }
    
    @PreDestroy
    public void stop() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
        if (consumer != null) {
            consumer.close();
        }
    }
}
