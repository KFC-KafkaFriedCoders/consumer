package org.example.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KafkaSalesMinuteService {

    private static final String BOOTSTRAP_SERVERS = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
    private static final String TOPIC = "sales_minute-minute";
    private static final String GROUP_ID = "sales-minute-consumer-group";

    private final WebSocketService webSocketService;
    private final BrandDataManager brandDataManager;
    private final Executor salesMinuteExecutor;
    
    @Value("${app.performance.enable-backpressure:true}")
    private boolean enableBackpressure;
    
    @Value("${app.performance.max-pending-tasks:5000}")
    private int maxPendingTasks;
    
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong droppedMessages = new AtomicLong(0);
    
    private final BlockingQueue<JSONObject> messageQueue = new LinkedBlockingQueue<>(10000);

    @Autowired
    public KafkaSalesMinuteService(
            WebSocketService webSocketService, 
            BrandDataManager brandDataManager,
            @Qualifier("salesMinuteExecutor") Executor salesMinuteExecutor) {
        this.webSocketService = webSocketService;
        this.brandDataManager = brandDataManager;
        this.salesMinuteExecutor = salesMinuteExecutor;
    }

    @PostConstruct
    public void start() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");
        properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1024");

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(TOPIC));

        System.out.println("SalesMinuteService 시작됨 - 토픽: " + TOPIC);

        executorService = Executors.newFixedThreadPool(3);
        executorService.submit(this::pollMessages);
        executorService.submit(this::processMessages);
        executorService.submit(this::printStats);
    }

    private void pollMessages() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        String jsonValue = record.value();
                        JSONObject jsonObject = new JSONObject(jsonValue);
                        
                        if (enableBackpressure && messageQueue.size() > maxPendingTasks) {
                            droppedMessages.incrementAndGet();
                            continue;
                        }
                        
                        if (!messageQueue.offer(jsonObject)) {
                            droppedMessages.incrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        System.err.println("SalesMinute JSON 파싱 오류: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("SalesMinute 메시지 폴링 중 오류: " + e.getMessage());
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    private void processMessages() {
        try {
            while (running.get()) {
                JSONObject message = messageQueue.poll();
                if (message != null) {
                    processMessageAsync(message);
                    processedMessages.incrementAndGet();
                } else {
                    Thread.sleep(10);
                }
            }
        } catch (Exception e) {
            System.err.println("메시지 처리 중 오류: " + e.getMessage());
        }
    }

    @Async("salesMinuteExecutor")
    public void processMessageAsync(JSONObject jsonObject) {
        try {
            String brand = jsonObject.optString("store_brand", "");
            if (brandDataManager.isValidBrand(brand)) {
                brandDataManager.updateSalesMinuteData(jsonObject);
                webSocketService.sendSalesMinute(jsonObject);
            }
        } catch (Exception e) {
            System.err.println("비동기 메시지 처리 오류: " + e.getMessage());
        }
    }

    private void printStats() {
        try {
            while (running.get()) {
                Thread.sleep(30000);
                
                long processed = processedMessages.get();
                long dropped = droppedMessages.get();
                int queueSize = messageQueue.size();
                
                if (processed > 0 || dropped > 0) {
                    System.out.println(String.format(
                        "SalesMinute Stats - Processed: %d, Dropped: %d, Queue: %d, Success Rate: %.2f%%",
                        processed, dropped, queueSize, 
                        (double) processed / (processed + dropped) * 100
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("통계 출력 중 오류: " + e.getMessage());
        }
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
        System.out.println("KafkaSalesMinuteService 중지됨");
    }
}