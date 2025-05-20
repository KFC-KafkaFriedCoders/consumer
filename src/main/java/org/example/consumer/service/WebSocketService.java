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
    private final UnifiedBrandFilterService unifiedBrandFilterService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate, 
                          BrandDataManager brandDataManager,
                          UnifiedBrandFilterService unifiedBrandFilterService) {
        this.messagingTemplate = messagingTemplate;
        this.brandDataManager = brandDataManager;
        this.unifiedBrandFilterService = unifiedBrandFilterService;
    }
    
    public SimpMessagingTemplate getMessagingTemplate() {
        return messagingTemplate;
    }

    public void sendPaymentLimitAlert(JSONObject paymentData) {
        paymentData.put("server_received_time", LocalDateTime.now().format(formatter));
        paymentData.put("event_type", "payment_limit_alert");
        
        if (!paymentData.has("id")) {
            paymentData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        brandDataManager.addPaymentLimitData(paymentData);
        
        String brand = paymentData.optString("store_brand", "");
        if (brandDataManager.isValidBrand(brand)) {
            unifiedBrandFilterService.notifyBrandUsers(brand, paymentData, "/topic/brand-payment-limit-update", "payment_limit_alert");
        }
    }

    public void sendSameUserAlert(JSONObject sameUserData) {
        sameUserData.put("server_received_time", LocalDateTime.now().format(formatter));
        sameUserData.put("event_type", "payment_same_user_alert");
        
        if (!sameUserData.has("id")) {
            sameUserData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        brandDataManager.addSameUserData(sameUserData);
        
        String brand = sameUserData.optString("store_brand", "");
        if (brandDataManager.isValidBrand(brand)) {
            unifiedBrandFilterService.notifyBrandUsers(brand, sameUserData, "/topic/brand-same-user-update", "payment_same_user_alert");
        }
    }
    
    public void sendSalesTotalData(JSONObject salesData) {
        salesData.put("server_received_time", LocalDateTime.now().format(formatter));
        salesData.put("event_type", "sales_total_update");
        
        if (!salesData.has("id")) {
            salesData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        brandDataManager.updateSalesTotalData(salesData);
        unifiedBrandFilterService.processNewSalesData(salesData);
    }
    
    public void sendTopStoresData(JSONObject topStoresData) {
        topStoresData.put("server_received_time", LocalDateTime.now().format(formatter));
        topStoresData.put("event_type", "top_stores_update");
        
        if (!topStoresData.has("id")) {
            topStoresData.put("id", System.currentTimeMillis() + "-" + Math.random());
        }
        
        brandDataManager.updateTopStoresData(topStoresData);
        unifiedBrandFilterService.processNewTopStoresData(topStoresData);
    }
    
    @Async("websocketExecutor")
    public CompletableFuture<Void> sendServerStatusAsync(String status) {
        return CompletableFuture.runAsync(() -> {
            JSONObject statusData = new JSONObject();
            statusData.put("event_type", "server_status");
            statusData.put("status", status);
            statusData.put("time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSend("/topic/server-status", statusData.toString());
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
            
            System.out.println("[WS 전송] 결제한도 | 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터: " + brandData.size() + "개");
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-payment-limit", response.toString());
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
            
            System.out.println("[WS 전송] 동일인결제 | 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터: " + brandData.size() + "개");
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-same-user", response.toString());
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
            
            System.out.println("[WS 전송] 매출총계 | 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터: " + (brandData != null ? "O" : "X"));
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-sales-total", response.toString());
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
            
            System.out.println("[WS 전송] TOP매장 | 브랜드: " + brand + ", 세션: " + sessionId + ", 데이터: " + (brandData != null ? "O" : "X"));
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-top-stores", response.toString());
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
            
            System.out.println("[WS 전송] 브랜드목록 | 세션: " + sessionId + ", 브랜드: " + brands.size() + "개");
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/all-brands", response.toString());
        });
    }

    public void sendAllBrandData(String sessionId) {
        sendAllBrandDataAsync(sessionId);
    }
}
