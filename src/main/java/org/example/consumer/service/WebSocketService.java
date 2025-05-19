package org.example.consumer.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BrandDataManager brandDataManager;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate, BrandDataManager brandDataManager) {
        this.messagingTemplate = messagingTemplate;
        this.brandDataManager = brandDataManager;
    }
    
    public SimpMessagingTemplate getMessagingTemplate() {
        return messagingTemplate;
    }

    @Async("websocketExecutor")
    public CompletableFuture<Void> sendPaymentLimitBatchAsync(List<JSONObject> paymentDataList) {
        return CompletableFuture.runAsync(() -> {
            JSONObject batchMessage = new JSONObject();
            batchMessage.put("event_type", "payment_limit_batch");
            batchMessage.put("messages", new JSONArray(paymentDataList));
            batchMessage.put("count", paymentDataList.size());
            batchMessage.put("batch_time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSend("/topic/payment-limit", batchMessage.toString());
            System.out.println("[WebSocket 전송] 결제한도 배치 전송 - 메시지 수: " + paymentDataList.size() + ", 토픽: /topic/payment-limit");
        });
    }

    @Async("websocketExecutor")
    public CompletableFuture<Void> sendSameUserBatchAsync(List<JSONObject> sameUserDataList) {
        return CompletableFuture.runAsync(() -> {
            JSONObject batchMessage = new JSONObject();
            batchMessage.put("event_type", "same_user_batch");
            batchMessage.put("messages", new JSONArray(sameUserDataList));
            batchMessage.put("count", sameUserDataList.size());
            batchMessage.put("batch_time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSend("/topic/payment-same-user", batchMessage.toString());
            System.out.println("[WebSocket 전송] 동일인결제 배치 전송 - 메시지 수: " + sameUserDataList.size() + ", 토픽: /topic/payment-same-user");
        });
    }

    @Async("websocketExecutor")
    public CompletableFuture<Void> sendSalesTotalAsync(JSONObject salesData) {
        return CompletableFuture.runAsync(() -> {
            messagingTemplate.convertAndSend("/topic/sales-total", salesData.toString());
            String brand = salesData.optString("store_brand", "Unknown");
            System.out.println("[WebSocket 전송] 매출총합 데이터 전송 - 브랜드: " + brand + ", 토픽: /topic/sales-total");
        });
    }

    @Async("websocketExecutor")
    public CompletableFuture<Void> sendTopStoresAsync(JSONObject topStoresData) {
        return CompletableFuture.runAsync(() -> {
            messagingTemplate.convertAndSend("/topic/top-stores", topStoresData.toString());
            String brand = topStoresData.optString("store_brand", "Unknown");
            System.out.println("[WebSocket 전송] Top매장 데이터 전송 - 브랜드: " + brand + ", 토픽: /topic/top-stores");
        });
    }

    @Async("websocketExecutor")
    public CompletableFuture<Void> sendPaymentLimitAlertAsync(JSONObject paymentData) {
        return CompletableFuture.runAsync(() -> {
            paymentData.put("server_received_time", LocalDateTime.now().format(formatter));
            paymentData.put("event_type", "payment_limit_alert");
            
            if (!paymentData.has("id")) {
                paymentData.put("id", System.currentTimeMillis() + Math.random());
            }
            
            brandDataManager.addPaymentLimitData(paymentData);
            messagingTemplate.convertAndSend("/topic/payment-limit", paymentData.toString());
            
            String brand = paymentData.optString("store_brand", "Unknown");
            System.out.println("[WebSocket 전송] 결제한도 알림 전송 - 브랜드: " + brand + ", ID: " + paymentData.opt("id"));
        });
    }

    @Async("websocketExecutor")
    public CompletableFuture<Void> sendSameUserAlertAsync(JSONObject sameUserData) {
        return CompletableFuture.runAsync(() -> {
            sameUserData.put("server_received_time", LocalDateTime.now().format(formatter));
            sameUserData.put("event_type", "payment_same_user_alert");
            
            if (!sameUserData.has("id")) {
                sameUserData.put("id", System.currentTimeMillis() + Math.random());
            }
            
            brandDataManager.addSameUserData(sameUserData);
            messagingTemplate.convertAndSend("/topic/payment-same-user", sameUserData.toString());
            
            String brand = sameUserData.optString("store_brand", "Unknown");
            System.out.println("[WebSocket 전송] 동일인결제 알림 전송 - 브랜드: " + brand + ", ID: " + sameUserData.opt("id"));
        });
    }

    public void sendPaymentLimitAlert(JSONObject paymentData) {
        sendPaymentLimitAlertAsync(paymentData);
    }
    
    public void sendSameUserAlert(JSONObject sameUserData) {
        sendSameUserAlertAsync(sameUserData);
    }
    
    public void sendSalesTotalData(JSONObject salesData) {
        salesData.put("server_received_time", LocalDateTime.now().format(formatter));
        salesData.put("event_type", "sales_total_update");
        
        if (!salesData.has("id")) {
            salesData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        brandDataManager.updateSalesTotalData(salesData);
        sendSalesTotalAsync(salesData);
    }
    
    public void sendTopStoresData(JSONObject topStoresData) {
        topStoresData.put("server_received_time", LocalDateTime.now().format(formatter));
        topStoresData.put("event_type", "top_stores_update");
        
        if (!topStoresData.has("id")) {
            topStoresData.put("id", System.currentTimeMillis() + "-" + Math.random());
        }
        
        brandDataManager.updateTopStoresData(topStoresData);
        sendTopStoresAsync(topStoresData);
    }
    
    @Async("websocketExecutor")
    public CompletableFuture<Void> sendServerStatusAsync(String status) {
        return CompletableFuture.runAsync(() -> {
            JSONObject statusData = new JSONObject();
            statusData.put("event_type", "server_status");
            statusData.put("status", status);
            statusData.put("time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSend("/topic/server-status", statusData.toString());
            System.out.println("[WebSocket 전송] 서버 상태 메시지 - 내용: " + status);
        });
    }
    
    public void sendServerStatus(String status) {
        sendServerStatusAsync(status);
    }
    
    @Async("websocketExecutor")
    public CompletableFuture<Void> sendBrandPaymentLimitDataAsync(String brand, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            List<JSONObject> brandData = brandDataManager.getPaymentLimitData(brand);
            
            JSONObject response = new JSONObject();
            response.put("event_type", "brand_payment_limit_data");
            response.put("brand", brand);
            
            JSONArray dataArray = new JSONArray();
            for (JSONObject data : brandData) {
                dataArray.put(data);
            }
            response.put("data", dataArray);
            response.put("count", brandData.size());
            response.put("server_sent_time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-payment-limit", response.toString());
            System.out.println("[WebSocket 전송] 브랜드별 결제한도 데이터 - 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터 수: " + brandData.size());
        });
    }

    public void sendBrandPaymentLimitData(String brand, String sessionId) {
        sendBrandPaymentLimitDataAsync(brand, sessionId);
    }
    
    @Async("websocketExecutor")
    public CompletableFuture<Void> sendBrandSameUserDataAsync(String brand, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            List<JSONObject> brandData = brandDataManager.getSameUserData(brand);
            
            JSONObject response = new JSONObject();
            response.put("event_type", "brand_same_user_data");
            response.put("brand", brand);
            
            JSONArray dataArray = new JSONArray();
            for (JSONObject data : brandData) {
                dataArray.put(data);
            }
            response.put("data", dataArray);
            response.put("count", brandData.size());
            response.put("server_sent_time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-same-user", response.toString());
            System.out.println("[WebSocket 전송] 브랜드별 동일인결제 데이터 - 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터 수: " + brandData.size());
        });
    }

    public void sendBrandSameUserData(String brand, String sessionId) {
        sendBrandSameUserDataAsync(brand, sessionId);
    }
    
    @Async("websocketExecutor")
    public CompletableFuture<Void> sendBrandSalesTotalDataAsync(String brand, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            JSONObject brandData = brandDataManager.getSalesTotalData(brand);
            
            JSONObject response = new JSONObject();
            response.put("event_type", "brand_sales_total_data");
            response.put("brand", brand);
            
            if (brandData != null) {
                response.put("data", brandData);
                response.put("hasData", true);
            } else {
                response.put("data", JSONObject.NULL);
                response.put("hasData", false);
            }
            response.put("server_sent_time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-sales-total", response.toString());
            System.out.println("[WebSocket 전송] 브랜드별 매출총합 데이터 - 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터 존재: " + (brandData != null));
        });
    }

    public void sendBrandSalesTotalData(String brand, String sessionId) {
        sendBrandSalesTotalDataAsync(brand, sessionId);
    }
    
    @Async("websocketExecutor")
    public CompletableFuture<Void> sendBrandTopStoresDataAsync(String brand, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            JSONObject brandData = brandDataManager.getTopStoresData(brand);
            
            JSONObject response = new JSONObject();
            response.put("event_type", "brand_top_stores_data");
            response.put("brand", brand);
            
            if (brandData != null) {
                response.put("data", brandData);
                response.put("hasData", true);
            } else {
                response.put("data", JSONObject.NULL);
                response.put("hasData", false);
            }
            response.put("server_sent_time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-top-stores", response.toString());
            System.out.println("[WebSocket 전송] 브랜드별 Top매장 데이터 - 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터 존재: " + (brandData != null));
        });
    }

    public void sendBrandTopStoresData(String brand, String sessionId) {
        sendBrandTopStoresDataAsync(brand, sessionId);
    }
    
    @Async("websocketExecutor")
    public CompletableFuture<Void> sendAllBrandDataAsync(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            List<String> brands = brandDataManager.getAllBrands();
            
            JSONObject response = new JSONObject();
            response.put("event_type", "all_brands_list");
            
            JSONArray brandsArray = new JSONArray();
            for (String brand : brands) {
                brandsArray.put(brand);
            }
            response.put("brands", brandsArray);
            response.put("count", brands.size());
            response.put("server_sent_time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/all-brands", response.toString());
            System.out.println("[WebSocket 전송] 모든 브랜드 목록 - 세션: " + sessionId + ", 브랜드 수: " + brands.size());
        });
    }

    public void sendAllBrandData(String sessionId) {
        sendAllBrandDataAsync(sessionId);
    }
}