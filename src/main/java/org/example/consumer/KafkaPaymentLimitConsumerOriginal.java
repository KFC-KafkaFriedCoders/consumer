package org.example.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * 원본 Kafka 컨슈머 클래스 - 참조용 백업
 */
public class KafkaPaymentLimitConsumerOriginal {

    public static void main(String[] args) {
        // Kafka 브로커 주소
        String bootstrapServers = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
        
        // 구독할 토픽
        String topic = "payment_limit";
        
        // 컨슈머 그룹 ID
        String groupId = "payment-limit-consumer-group";
        
        // 컨슈머 속성 설정
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 오프셋이 없는 경우 가장 초기 메시지부터 시작
        // 자동 커밋 설정
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        System.out.println("PaymentLimitConsumer 시작 중...");
        System.out.println("브로커 주소: " + bootstrapServers);
        System.out.println("구독 토픽: " + topic);
        System.out.println("컨슈머 그룹 ID: " + groupId);
        
        // 컨슈머 생성 및 무한 루프로 구독 유지
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            // 토픽 구독
            consumer.subscribe(Collections.singletonList(topic));
            
            System.out.println("토픽 구독 시작. 메시지 대기 중...");
            System.out.println("프로그램을 종료하려면 Ctrl+C를 누르세요.");
            
            // 무한 루프로 계속 폴링
            while (true) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            String jsonValue = record.value();
                            JSONObject jsonObject = new JSONObject(jsonValue);
                            
                            System.out.println("== 파싱된 JSON 데이터 ==");
                            System.out.println("franchise_id: " + jsonObject.getInt("franchise_id"));
                            System.out.println("store_brand: " + jsonObject.getString("store_brand"));
                            System.out.println("store_id: " + jsonObject.getInt("store_id"));
                            System.out.println("store_name: " + jsonObject.getString("store_name"));
                            System.out.println("region: " + jsonObject.getString("region"));
                            System.out.println("store_address: " + jsonObject.getString("store_address"));
                            System.out.println("total_price: " + jsonObject.getInt("total_price"));
                            System.out.println("user_id: " + jsonObject.getInt("user_id"));
                            System.out.println("time: " + jsonObject.getString("time"));
                            System.out.println("user_name: " + jsonObject.getString("user_name"));
                            System.out.println("user_gender: " + jsonObject.getString("user_gender"));
                            System.out.println("user_age: " + jsonObject.getInt("user_age"));
                            System.out.println("alert_message: " + jsonObject.getString("alert_message"));
                        } catch (Exception e) {
                            System.err.println("JSON 파싱 오류: " + e.getMessage());
                            System.out.println("원본 값: " + record.value());
                        }
                        System.out.println("------------------------------------");
                    }
                } catch (Exception e) {
                    System.err.println("메시지 처리 중 오류 발생: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Kafka 소비자 초기화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
