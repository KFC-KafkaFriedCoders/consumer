package org.example.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class KafkaSalesMinuteService {

    private static final String BOOTSTRAP_SERVERS = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
    private static final String TOPIC = "sales_minute-minute";
    private static final String GROUP_ID = "sales-minute-consumer-group";

    private final WebSocketService webSocketService;
    private final BrandDataManager brandDataManager;
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    public KafkaSalesMinuteService(WebSocketService webSocketService, BrandDataManager brandDataManager) {
        this.webSocketService = webSocketService;
        this.brandDataManager = brandDataManager;
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

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(TOPIC));

        System.out.println("SalesMinuteService 시작됨 - 토픽: " + TOPIC);

        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::pollMessages);
    }

    private void pollMessages() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        String jsonValue = record.value();
                        JSONObject jsonObject = new JSONObject(jsonValue);

                        updateSalesMinuteDataSync(jsonObject);
                        sendSalesMinuteSync(jsonObject);
                        
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

    private void updateSalesMinuteDataSync(JSONObject jsonObject) {
        try {
            String brand = jsonObject.optString("store_brand", "");
            if (brandDataManager.isValidBrand(brand)) {
                brandDataManager.updateSalesMinuteDataSync(jsonObject);
            }
        } catch (Exception e) {
            System.err.println("BrandDataManager 업데이트 오류: " + e.getMessage());
        }
    }

    private void sendSalesMinuteSync(JSONObject jsonObject) {
        try {
            webSocketService.sendSalesMinuteSync(jsonObject);
        } catch (Exception e) {
            System.err.println("WebSocket 전송 오류: " + e.getMessage());
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
    }
}
