package org.example.consumer.service;

import org.example.consumer.config.properties.DataProperties;
import org.example.consumer.service.interfaces.BatchProcessorService;
import org.example.consumer.service.interfaces.MessageType;
import org.example.consumer.service.interfaces.WebSocketNotificationService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WebSocketService implements WebSocketNotificationService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final BatchProcessorService batchProcessor;
    private final DateTimeFormatter formatter;

    @Autowired
    public WebSocketService(
            SimpMessagingTemplate messagingTemplate, 
            BatchProcessorService batchProcessor,
            DataProperties dataProperties) {
        this.messagingTemplate = messagingTemplate;
        this.batchProcessor = batchProcessor;
        this.formatter = DateTimeFormatter.ofPattern(dataProperties.getDateFormat());
    }
    
    public SimpMessagingTemplate getMessagingTemplate() {
        return messagingTemplate;
    }

    @Async("taskExecutor")
    public void sendPaymentLimitAlert(JSONObject paymentData) {
        try {
            if (paymentData == null) {
                log.warn("결제 한도 알림 데이터가 null입니다.");
                return;
            }
            
            // 방어적 복사
            JSONObject safeData = new JSONObject();
            for (String key : paymentData.keySet()) {
                safeData.put(key, paymentData.get(key));
            }
            
            safeData.put("server_received_time", LocalDateTime.now().format(formatter));
            safeData.put("event_type", "payment_limit_alert");
            
            if (!safeData.has("id")) {
                safeData.put("id", System.currentTimeMillis() + Math.random());
            }
            
            batchProcessor.addToQueue(MessageType.PAYMENT_LIMIT, safeData);
            log.debug("결제 한도 알림 전송: {}", safeData.optString("store_brand", "unknown"));
        } catch (Exception e) {
            log.error("결제 한도 알림 전송 중 오류: {}", e.getMessage());
        }
    }
    
    @Async("taskExecutor")
    public void sendSameUserAlert(JSONObject sameUserData) {
        try {
            if (sameUserData == null) {
                log.warn("동일 사용자 알림 데이터가 null입니다.");
                return;
            }
            
            // 방어적 복사
            JSONObject safeData = new JSONObject();
            for (String key : sameUserData.keySet()) {
                safeData.put(key, sameUserData.get(key));
            }
            
            safeData.put("server_received_time", LocalDateTime.now().format(formatter));
            safeData.put("event_type", "payment_same_user_alert");
            
            if (!safeData.has("id")) {
                safeData.put("id", System.currentTimeMillis() + Math.random());
            }
            
            batchProcessor.addToQueue(MessageType.SAME_USER, safeData);
            log.debug("동일 사용자 결제 알림 전송: {}", safeData.optString("store_brand", "unknown"));
        } catch (Exception e) {
            log.error("동일 사용자 알림 전송 중 오류: {}", e.getMessage());
        }
    }
    
    @Async("taskExecutor")
    public void sendServerStatus(String status) {
        try {
            JSONObject statusData = new JSONObject();
            statusData.put("event_type", "server_status");
            statusData.put("status", status);
            statusData.put("time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSend("/topic/server-status", statusData.toString());
            log.debug("서버 상태 메시지 전송: {}", status);
        } catch (Exception e) {
            log.error("서버 상태 메시지 전송 중 오류: {}", e.getMessage());
        }
    }
    
    @Async("taskExecutor")
    public void sendSalesTotalData(JSONObject salesData) {
        try {
            if (salesData == null) {
                log.warn("매출 총합 데이터가 null입니다.");
                return;
            }
            
            // 방어적 복사
            JSONObject safeData = new JSONObject();
            for (String key : salesData.keySet()) {
                safeData.put(key, salesData.get(key));
            }
            
            safeData.put("server_received_time", LocalDateTime.now().format(formatter));
            safeData.put("event_type", "sales_total_update");
            
            if (!safeData.has("id")) {
                safeData.put("id", System.currentTimeMillis() + Math.random());
            }
            
            batchProcessor.addToQueue(MessageType.SALES_TOTAL, safeData);
            log.debug("매출 데이터 전송: {}", safeData.optString("store_brand", "unknown"));
        } catch (Exception e) {
            log.error("매출 총합 데이터 전송 중 오류: {}", e.getMessage());
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
    
    @Async("taskExecutor")
    public void sendTopStoresData(JSONObject topStoresData) {
        try {
            if (topStoresData == null) {
                log.warn("Top Stores 데이터가 null입니다.");
                return;
            }
            
            // 추가 디버깅
            log.debug("TopStores 데이터 전송 전 검증 - 키셋: {}", topStoresData.keySet());
            if (topStoresData.has("top_stores")) {
                log.debug("top_stores 배열 크기: {}", topStoresData.getJSONArray("top_stores").length());
            } else {
                log.warn("top_stores 배열이 없습니다!");
                return;
            }
            
            // 깊은 복사를 통한 안전한 복사본 생성
            JSONObject safeData = deepCopyJsonObject(topStoresData);
            
            // 검증
            if (safeData == null) {
                log.warn("Top Stores 복사본이 null입니다.");
                return;
            }
            
            safeData.put("server_received_time", LocalDateTime.now().format(formatter));
            safeData.put("event_type", "top_stores_update");
            
            if (!safeData.has("id")) {
                safeData.put("id", System.currentTimeMillis() + "-" + Math.random());
            }
            
            // 브랜드 정보 로깅
            String brand = "unknown";
            if (safeData.has("store_brand")) {
                brand = safeData.getString("store_brand");
            } else if (safeData.has("top_stores") && safeData.getJSONArray("top_stores").length() > 0) {
                JSONObject firstStore = safeData.getJSONArray("top_stores").getJSONObject(0);
                if (firstStore.has("store_brand")) {
                    brand = firstStore.getString("store_brand");
                    
                    // 만약 원본에 store_brand가 없다면 추가
                    if (!safeData.has("store_brand")) {
                        safeData.put("store_brand", brand);
                    }
                }
            }
            
            // 복사본을 큐에 전달
            batchProcessor.addToQueue(MessageType.TOP_STORES, safeData);
            log.debug("Top Stores 데이터 전송: {}", brand);
        } catch (Exception e) {
            log.error("Top Stores 데이터 전송 중 오류: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Async("taskExecutor")
    public void sendNonResponseData(JSONObject nonResponseData) {
        try {
            if (nonResponseData == null) {
                log.warn("비응답 매장 데이터가 null입니다.");
                return;
            }
            
            JSONObject safeData = new JSONObject();
            for (String key : nonResponseData.keySet()) {
                safeData.put(key, nonResponseData.get(key));
            }
            
            safeData.put("server_received_time", LocalDateTime.now().format(formatter));
            safeData.put("event_type", "non_response_data");
            
            if (!safeData.has("id")) {
                safeData.put("id", System.currentTimeMillis() + "-" + Math.random());
            }
            
            messagingTemplate.convertAndSend("/topic/non-response", safeData.toString());
            log.debug("비응답 매장 데이터 전송: {}", safeData.optString("store_brand", "unknown"));
        } catch (Exception e) {
            log.error("비응답 매장 데이터 전송 중 오류: {}", e.getMessage());
        }
    }

    @Async("salesMinuteExecutor")
    public void sendSalesMinute(JSONObject salesMinuteData) {
        sendSalesMinuteSync(salesMinuteData);
    }

    public void sendSalesMinuteSync(JSONObject salesMinuteData) {
        try {
            if (salesMinuteData == null) {
                log.warn("분별 매출 데이터가 null입니다.");
                return;
            }
            
            JSONObject safeData = new JSONObject();
            safeData.put("franchise_id", salesMinuteData.opt("franchise_id"));
            safeData.put("store_brand", salesMinuteData.opt("store_brand"));
            safeData.put("store_count", salesMinuteData.opt("store_count"));
            safeData.put("total_sales", salesMinuteData.opt("total_sales"));
            safeData.put("update_time", salesMinuteData.opt("update_time"));
            safeData.put("server_received_time", LocalDateTime.now().format(formatter));
            safeData.put("event_type", "sales_minute_update");
            safeData.put("id", System.currentTimeMillis() + "-" + Thread.currentThread().getId());
            
            messagingTemplate.convertAndSend("/topic/sales-minute", safeData.toString());
            log.debug("분별 매출 데이터 전송: {}", safeData.optString("store_brand", "unknown"));
        } catch (Exception e) {
            log.error("분별 매출 데이터 전송 중 오류: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendBrandPaymentLimitData(String brand, String sessionId) {
        // 이 메서드는 더 이상 BrandManager에 직접 의존하지 않음
        // 대신 필요한 데이터는 이 서비스로 전달되어야 함
        log.debug("API 호출: 브랜드별 결제 한도 데이터 요청 - 브랜드: {}", brand);
    }
    
    @Async("taskExecutor")
    public void sendBrandSameUserData(String brand, String sessionId) {
        // 이 메서드는 더 이상 BrandManager에 직접 의존하지 않음
        log.debug("API 호출: 브랜드별 동일인 결제 데이터 요청 - 브랜드: {}", brand);
    }
    
    @Async("taskExecutor")
    public void sendBrandSalesTotalData(String brand, String sessionId) {
        // 이 메서드는 더 이상 BrandManager에 직접 의존하지 않음
        log.debug("API 호출: 브랜드별 매출 총합 데이터 요청 - 브랜드: {}", brand);
    }
    
    @Async("taskExecutor")
    public void sendBrandTopStoresData(String brand, String sessionId) {
        // 이 메서드는 더 이상 BrandManager에 직접 의존하지 않음
        log.debug("API 호출: 브랜드별 Top Stores 데이터 요청 - 브랜드: {}", brand);
    }
    
    @Async("taskExecutor")
    public void sendAllBrandData(String sessionId) {
        // 이 메서드는 더 이상 BrandManager에 직접 의존하지 않음
        log.debug("API 호출: 모든 브랜드 목록 요청");
    }
}
