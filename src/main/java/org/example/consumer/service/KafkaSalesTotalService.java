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
public class KafkaSalesTotalService {

    // Kafka 설정
    private static final String BOOTSTRAP_SERVERS = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
    private static final String TOPIC = "sales_total_realtime";
    private static final String GROUP_ID = "sales-total-consumer-group";

    private final WebSocketService webSocketService;
    private final BrandFilterService brandFilterService;
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    public KafkaSalesTotalService(WebSocketService webSocketService, BrandFilterService brandFilterService) {
        this.webSocketService = webSocketService;
        this.brandFilterService = brandFilterService;
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

        System.out.println("KafkaSalesTotalService 시작 중...");
        System.out.println("브로커 주소: " + BOOTSTRAP_SERVERS);
        System.out.println("구독 토픽: " + TOPIC);
        System.out.println("컨슈머 그룹 ID: " + GROUP_ID);
        
        // WebSocket으로 서버 시작 상태 전송
        webSocketService.sendServerStatus("Kafka Sales Total Consumer 서비스가 시작되었습니다. 토픽: " + TOPIC);

        // 백그라운드 스레드에서 Kafka 메시지 폴링
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

                        // 콘솔에 로그 출력
                        System.out.println("------------------------------------");
                        System.out.println("판매 데이터 수신: " + jsonObject.toString());
                        logSalesData(jsonObject);
                        
                        // WebSocket을 통해 클라이언트에게 메시지 전송 (브랜드 필터링 적용)
                        brandFilterService.processNewData(jsonObject);
                    } catch (Exception e) {
                        System.err.println("JSON 파싱 오류: " + e.getMessage());
                        System.out.println("원본 값: " + record.value());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("메시지 폴링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            
            // WebSocket으로 오류 상태 전송
            webSocketService.sendServerStatus("Kafka Sales Total Consumer 오류 발생: " + e.getMessage());
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }
    
    // 로깅을 위한 헬퍼 메서드
    private void logSalesData(JSONObject jsonObject) {
        /*
        System.out.println("가맹점 ID: " + jsonObject.getInt("franchise_id"));
        System.out.println("브랜드명: " + jsonObject.getString("store_brand"));
        System.out.println("매장 수: " + jsonObject.getInt("store_count"));
        System.out.println("총 매출: " + jsonObject.getLong("total_sales"));
        System.out.println("업데이트 시간: " + jsonObject.getString("update_time"));*/
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
        
        // WebSocket으로 서버 종료 상태 전송
        webSocketService.sendServerStatus("Kafka Sales Total Consumer 서비스가 종료되었습니다.");
        System.out.println("KafkaSalesTotalService 중지됨");
    }
}
