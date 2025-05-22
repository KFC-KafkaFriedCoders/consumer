package org.example.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONArray;
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
public class KafkaTopStoresService {

    // Kafka 설정
    private static final String BOOTSTRAP_SERVERS = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
    private static final String TOPIC = "franchise-top-stores-new";
    private static final String GROUP_ID = "top-stores-consumer-group";

    private final WebSocketService webSocketService;
    private final BrandDataManager brandDataManager;
    private final TopStoresFilterService topStoresFilterService;
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    public KafkaTopStoresService(WebSocketService webSocketService, 
                                 BrandDataManager brandDataManager,
                                 TopStoresFilterService topStoresFilterService) {
        this.webSocketService = webSocketService;
        this.brandDataManager = brandDataManager;
        this.topStoresFilterService = topStoresFilterService;
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

        System.out.println("KafkaTopStoresService 시작 중...");
        System.out.println("브로커 주소: " + BOOTSTRAP_SERVERS);
        System.out.println("구독 토픽: " + TOPIC);
        System.out.println("컨슈머 그룹 ID: " + GROUP_ID);
        
        // WebSocket으로 서버 시작 상태 전송
        webSocketService.sendServerStatus("Kafka Top Stores Consumer 서비스가 시작되었습니다. 토픽: " + TOPIC);

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
                        
                        // 추가 디버그: 원본 JSON 로깅
                        //System.out.println("받은 원본 JSON: " + jsonValue);
                        
                        // 유효성 체크
                        if (jsonValue == null || jsonValue.trim().isEmpty()) {
                            //System.err.println("빈 JSON 문자열이 수신되었습니다. 처리를 건너뜁니다.");
                            continue;
                        }
                        
                        JSONObject jsonObject = new JSONObject(jsonValue);

                        // 콘솔에 로그 출력
                        System.out.println("------------------------------------");
                        System.out.println("Top Stores 데이터 수신: " + jsonObject.toString());
                        
                        logTopStoresData(jsonObject);
                        
                        // 유효성 검사 추가
                        if (!jsonObject.has("top_stores")) {
                            // System.err.println("수신된 JSON에 top_stores 배열이 없습니다. 처리를 건너뜁니다.");
                            continue;
                        }
                        
                        if (jsonObject.getJSONArray("top_stores").length() == 0) {
                            // System.err.println("수신된 JSON에 top_stores 배열이 비어 있습니다. 처리를 건너뜁니다.");
                            continue;
                        }
                        
                        if (!jsonObject.getJSONArray("top_stores").getJSONObject(0).has("store_brand")) {
                            // System.err.println("첫 번째 매장에 store_brand 필드가 없습니다. 처리를 건너뜁니다.");
                            continue;
                        }
                        
                        // 딥 복사를 수행하여 새 객체 생성
                        JSONObject safeJsonObject = deepCopyJsonObject(jsonObject);
                        
                        // WebSocket을 통해 클라이언트에게 메시지 전송 (브랜드별 저장 포함)
                        webSocketService.sendTopStoresData(safeJsonObject);
                        
                        // 안전하게 새 인스턴스로 복사
                        JSONObject safeJsonObject2 = deepCopyJsonObject(jsonObject);
                        
                        // 기존 브랜드 필터링 서비스도 유지 (호환성을 위해)
                        topStoresFilterService.processNewData(safeJsonObject2);
                    } catch (Exception e) {
                        System.err.println("JSON 파싱 오류: " + e.getMessage());
                        e.printStackTrace();
                        System.out.println("원본 값: " + record.value());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("메시지 폴링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            
            // WebSocket으로 오류 상태 전송
            webSocketService.sendServerStatus("Kafka Top Stores Consumer 오류 발생: " + e.getMessage());
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }
    
    /**
     * JSONObject의 안전한 깊은 복사본을 생성합니다.
     */
    private JSONObject deepCopyJsonObject(JSONObject source) {
        if (source == null) {
            return null;
        }
        
        JSONObject copy = new JSONObject();
        
        // 기본 속성 복사
        for (String key : source.keySet()) {
            if (key.equals("top_stores")) {
                continue; // top_stores는 별도로 처리
            }
            
            Object value = source.get(key);
            copy.put(key, value);
        }
        
        // top_stores 배열 처리
        if (source.has("top_stores")) {
            JSONArray sourceStores = source.getJSONArray("top_stores");
            JSONArray copyStores = new JSONArray();
            
            for (int i = 0; i < sourceStores.length(); i++) {
                JSONObject sourceStore = sourceStores.getJSONObject(i);
                JSONObject copyStore = new JSONObject();
                
                for (String key : sourceStore.keySet()) {
                    Object value = sourceStore.get(key);
                    copyStore.put(key, value);
                }
                
                copyStores.put(copyStore);
            }
            
            copy.put("top_stores", copyStores);
        }
        
        return copy;
    }
    
    // 로깅을 위한 헬퍼 메서드
    private void logTopStoresData(JSONObject jsonObject) {
        try {
            if (jsonObject.has("franchise_id")) {
                System.out.println("franchise_id: " + jsonObject.getInt("franchise_id"));
            }
            if (jsonObject.has("timestamp")) {
                System.out.println("timestamp: " + jsonObject.getString("timestamp"));
            }
            if (jsonObject.has("top_stores")) {
                System.out.println("top_stores 개수: " + jsonObject.getJSONArray("top_stores").length());
            }
            
            // 원본 값을 출력할 때는 더 이상 로깅 오류를 발생시키지 않음
            // System.out.println("원본 값: " + jsonObject.toString());
        } catch (Exception e) {
            System.err.println("로깅 중 오류: " + e.getMessage());
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
        
        // WebSocket으로 서버 종료 상태 전송
        webSocketService.sendServerStatus("Kafka Top Stores Consumer 서비스가 종료되었습니다.");
        System.out.println("KafkaTopStoresService 중지됨");
    }
}
