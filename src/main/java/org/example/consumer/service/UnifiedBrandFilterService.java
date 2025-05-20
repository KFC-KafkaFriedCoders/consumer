package org.example.consumer.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UnifiedBrandFilterService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final Map<String, String> userBrandSelections = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<JSONObject>> brandDataCache = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> brandTopStoresCache = new ConcurrentHashMap<>();
    
    private static final int MAX_CACHE_SIZE_PER_BRAND = 30;
    
    public void registerUserBrandSelection(String sessionId, String brand) {
        userBrandSelections.put(sessionId, brand);
        sendBrandSelectionConfirmation(sessionId, brand);
        sendCachedBrandData(sessionId, brand);
        sendCachedTopStoresData(sessionId, brand);
    }
    
    public void clearUserBrandSelection(String sessionId) {
        userBrandSelections.remove(sessionId);
        sendBrandSelectionReset(sessionId);
    }
    
    public void removeUserSession(String sessionId) {
        userBrandSelections.remove(sessionId);
    }
    
    public void cacheSalesDataByBrand(JSONObject salesData) {
        String brand = salesData.getString("store_brand");
        JSONObject dataWithId = addIdIfMissing(new JSONObject(salesData.toString()));
        
        LinkedList<JSONObject> newBrandCache = new LinkedList<>();
        newBrandCache.add(dataWithId);
        brandDataCache.put(brand, newBrandCache);
    }
    
    public void cacheTopStoresDataByBrand(JSONObject topStoresData) {
        JSONArray topStores = topStoresData.getJSONArray("top_stores");
        if (topStores.length() > 0) {
            String brand = topStores.getJSONObject(0).getString("store_brand");
            JSONObject dataWithId = addIdIfMissing(new JSONObject(topStoresData.toString()));
            brandTopStoresCache.put(brand, dataWithId);
        }
    }
    
    public void processNewSalesData(JSONObject salesData) {
        String brand = salesData.getString("store_brand");
        cacheSalesDataByBrand(salesData);
        
        // 전체 토픽 제거, 브랜드별 사용자에게만 전송
        notifyBrandUsers(brand, salesData, "/topic/brand-sales-total-update", "sales_total_update");
    }
    
    public void processNewTopStoresData(JSONObject topStoresData) {
        JSONArray topStores = topStoresData.getJSONArray("top_stores");
        if (topStores.length() == 0) return;
        
        String brand = topStores.getJSONObject(0).getString("store_brand");
        cacheTopStoresDataByBrand(topStoresData);
        
        // 전체 토픽 제거, 브랜드별 사용자에게만 전송
        notifyBrandUsers(brand, topStoresData, "/topic/brand-top-stores-update", "top_stores_update");
    }
    
    public List<JSONObject> getRecentDataForBrand(String brand) {
        LinkedList<JSONObject> brandCache = brandDataCache.getOrDefault(brand, new LinkedList<>());
        return new ArrayList<>(brandCache);
    }
    
    public JSONObject getTopStoresDataForBrand(String brand) {
        return brandTopStoresCache.get(brand);
    }
    
    // WebSocketService에서 사용할 수 있도록 public으로 변경
    public void notifyBrandUsers(String brand, JSONObject originalData, String topic, String eventType) {
        int notifiedCount = 0;
        for (Map.Entry<String, String> entry : userBrandSelections.entrySet()) {
            String userSessionId = entry.getKey();
            String selectedBrand = entry.getValue();
            
            if (selectedBrand.equals(brand)) {
                JSONObject updateData = new JSONObject(originalData.toString());
                updateData.put("event_type", eventType);
                updateData.put("server_received_time", LocalDateTime.now().format(formatter));
                updateData = addIdIfMissing(updateData);
                
                messagingTemplate.convertAndSendToUser(userSessionId, topic, updateData.toString());
                notifiedCount++;
            }
        }
        
        if (notifiedCount > 0) {
            System.out.println("[WS 알림] " + eventType + " | 브랜드: " + brand + ", 사용자: " + notifiedCount + "명");
        }
    }
    
    private void sendBrandSelectionConfirmation(String sessionId, String brand) {
        JSONObject confirmation = new JSONObject();
        confirmation.put("event_type", "brand_selection_confirmation");
        confirmation.put("selected_brand", brand);
        confirmation.put("time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-selection", confirmation.toString());
    }
    
    private void sendBrandSelectionReset(String sessionId) {
        JSONObject confirmation = new JSONObject();
        confirmation.put("event_type", "brand_selection_reset");
        confirmation.put("time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-selection", confirmation.toString());
    }
    
    private void sendCachedBrandData(String sessionId, String brand) {
        List<JSONObject> cachedData = getRecentDataForBrand(brand);
        
        if (!cachedData.isEmpty()) {
            JSONObject batchData = new JSONObject();
            batchData.put("event_type", "brand_data_batch");
            batchData.put("brand", brand);
            batchData.put("items", cachedData);
            batchData.put("time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-data", batchData.toString());
        } else {
            sendEmptyData(sessionId, brand, "brand_data_empty", "/topic/brand-data");
        }
    }
    
    private void sendCachedTopStoresData(String sessionId, String brand) {
        JSONObject cachedData = brandTopStoresCache.get(brand);
        
        if (cachedData != null) {
            JSONObject batchData = new JSONObject();
            batchData.put("event_type", "top_stores_data_batch");
            batchData.put("brand", brand);
            batchData.put("data", cachedData);
            batchData.put("time", LocalDateTime.now().format(formatter));
            
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/top-stores-data", batchData.toString());
        } else {
            sendEmptyData(sessionId, brand, "top_stores_data_empty", "/topic/top-stores-data");
        }
    }
    
    private void sendEmptyData(String sessionId, String brand, String eventType, String topic) {
        JSONObject emptyData = new JSONObject();
        emptyData.put("event_type", eventType);
        emptyData.put("brand", brand);
        emptyData.put("message", "No data available for this brand");
        
        messagingTemplate.convertAndSendToUser(sessionId, topic, emptyData.toString());
    }
    
    private JSONObject addIdIfMissing(JSONObject data) {
        if (!data.has("id")) {
            data.put("id", System.currentTimeMillis() + "-" + Math.random());
        }
        return data;
    }
}
