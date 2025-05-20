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
        paymentData.put("server_received_time", LocalDateTime.now().format(formatter));
        paymentData.put("event_type", "payment_limit_alert");
        
        if (!paymentData.has("id")) {
            paymentData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        batchProcessor.addToQueue(MessageType.PAYMENT_LIMIT, paymentData);
        log.debug("결제 한도 알림 전송: {}", paymentData.optString("store_brand", "unknown"));
    }
    
    @Async("taskExecutor")
    public void sendSameUserAlert(JSONObject sameUserData) {
        sameUserData.put("server_received_time", LocalDateTime.now().format(formatter));
        sameUserData.put("event_type", "payment_same_user_alert");
        
        if (!sameUserData.has("id")) {
            sameUserData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        batchProcessor.addToQueue(MessageType.SAME_USER, sameUserData);
        log.debug("동일 사용자 결제 알림 전송: {}", sameUserData.optString("store_brand", "unknown"));
    }
    
    @Async("taskExecutor")
    public void sendServerStatus(String status) {
        JSONObject statusData = new JSONObject();
        statusData.put("event_type", "server_status");
        statusData.put("status", status);
        statusData.put("time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSend("/topic/server-status", statusData.toString());
        log.debug("서버 상태 메시지 전송: {}", status);
    }
    
    @Async("taskExecutor")
    public void sendSalesTotalData(JSONObject salesData) {
        salesData.put("server_received_time", LocalDateTime.now().format(formatter));
        salesData.put("event_type", "sales_total_update");
        
        if (!salesData.has("id")) {
            salesData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        batchProcessor.addToQueue(MessageType.SALES_TOTAL, salesData);
        log.debug("매출 데이터 전송: {}", salesData.optString("store_brand", "unknown"));
    }
    
    @Async("taskExecutor")
    public void sendTopStoresData(JSONObject topStoresData) {
        topStoresData.put("server_received_time", LocalDateTime.now().format(formatter));
        topStoresData.put("event_type", "top_stores_update");
        
        if (!topStoresData.has("id")) {
            topStoresData.put("id", System.currentTimeMillis() + "-" + Math.random());
        }
        
        batchProcessor.addToQueue(MessageType.TOP_STORES, topStoresData);
        log.debug("Top Stores 데이터 전송: {}", topStoresData.optString("store_brand", "unknown"));
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
