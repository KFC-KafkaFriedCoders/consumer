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
public class KafkaNonResponseService {

    private static final String BOOTSTRAP_SERVERS = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
    private static final String TOPIC = "3_non_response";
    private static final String GROUP_ID = "non-response-consumer-group1";

    private final WebSocketService webSocketService;
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    public KafkaNonResponseService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
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

        System.out.println("NonResponseService 시작 중...");
        System.out.println("브로커 주소: " + BOOTSTRAP_SERVERS);
        System.out.println("구독 토픽: " + TOPIC);
        System.out.println("컨슈머 그룹 ID: " + GROUP_ID);
        
        webSocketService.sendServerStatus("Kafka Consumer 서비스가 시작되었습니다. 토픽: " + TOPIC);

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

                        System.out.println("------------------------------------");
                        System.out.println("비응답 매장 데이터 수신: " + jsonObject.toString());

                        webSocketService.sendNonResponseData(jsonObject);
                        
                        String brand = jsonObject.optString("store_brand", "Unknown");
                        System.out.println("브랜드: " + brand + "의 비응답 매장 데이터 전송됨");
                    } catch (Exception e) {
                        System.err.println("JSON 파싱 오류: " + e.getMessage());
                        System.out.println("원본 값: " + record.value());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("메시지 폴링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            
            webSocketService.sendServerStatus("Kafka Consumer 오류 발생: " + e.getMessage());
        } finally {
            if (consumer != null) {
                consumer.close();
            }
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
        
        webSocketService.sendServerStatus("Kafka Consumer 서비스가 종료되었습니다.");
        System.out.println("KafkaNonResponseService 중지됨");
    }
}