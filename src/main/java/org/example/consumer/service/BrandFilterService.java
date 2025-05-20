package org.example.consumer.service;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrandFilterService {

    private static final Logger log = LoggerFactory.getLogger(BrandFilterService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    public BrandFilterService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    // 사용자 세션별 브랜드 선택 저장
    private final Map<String, String> userBrandSelections = new ConcurrentHashMap<>();
    
    // 브랜드별 최근 데이터 캐싱
    private final Map<String, LinkedList<JSONObject>> brandDataCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE_PER_BRAND = 30;
    
    /**
     * 사용자의 브랜드 선택을 등록합니다.
     */
    public void registerUserBrandSelection(String sessionId, String brand) {
        userBrandSelections.put(sessionId, brand);
        
        // 선택 확인 메시지 전송
        JSONObject confirmation = new JSONObject();
        confirmation.put("event_type", "brand_selection_confirmation");
        confirmation.put("selected_brand", brand);
        confirmation.put("time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSendToUser(
            sessionId, 
            "/topic/brand-selection", 
            confirmation.toString()
        );
        
        // 해당 브랜드의 캐시된 모든 데이터 전송
        sendCachedBrandData(sessionId, brand);
    }
    
    /**
     * 브랜드 선택 시 해당 브랜드의 모든 캐시된 데이터 전송
     */
    public void sendCachedBrandData(String sessionId, String brand) {
        List<JSONObject> cachedData = getRecentDataForBrand(brand);
        
        if (!cachedData.isEmpty()) {
            JSONObject batchData = new JSONObject();
            batchData.put("event_type", "brand_data_batch");
            batchData.put("brand", brand);
            batchData.put("items", cachedData);
            batchData.put("time", LocalDateTime.now().format(formatter));
            
            // 사용자에게 해당 브랜드의 모든 캐시된 데이터 한 번에 전송
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/topic/brand-data", 
                batchData.toString()
            );
            
            log.debug("브랜드 {}의 캐시된 데이터 {}개 전송", brand, cachedData.size());
        } else {
            // 데이터가 없을 경우 빈 데이터셋 전송
            JSONObject emptyData = new JSONObject();
            emptyData.put("event_type", "brand_data_empty");
            emptyData.put("brand", brand);
            emptyData.put("message", "No data available for this brand");
            
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/topic/brand-data", 
                emptyData.toString()
            );
        }
    }
    
    /**
     * 사용자의 브랜드 선택을 초기화합니다.
     */
    public void clearUserBrandSelection(String sessionId) {
        userBrandSelections.remove(sessionId);
        
        // 초기화 확인 메시지 전송
        JSONObject confirmation = new JSONObject();
        confirmation.put("event_type", "brand_selection_reset");
        confirmation.put("time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSendToUser(
            sessionId, 
            "/topic/brand-selection", 
            confirmation.toString()
        );
    }
    
    /**
     * 브랜드별 데이터를 캐시에 저장합니다.
     */
    public void cacheDataByBrand(JSONObject salesData) {
        String brand = salesData.getString("store_brand");
        
        LinkedList<JSONObject> brandCache = brandDataCache.computeIfAbsent(
            brand, k -> new LinkedList<>()
        );
        
        // 데이터에 고유 ID 추가 (클라이언트에서 NEW 배지 컴루터 용)
        JSONObject dataWithId = new JSONObject(salesData.toString());
        if (!dataWithId.has("id")) {
            dataWithId.put("id", System.currentTimeMillis() + Math.random());
        }
        
        // 각 브랜드별로 최신 데이터 1개만 저장
        LinkedList<JSONObject> newBrandCache = new LinkedList<>();
        newBrandCache.add(dataWithId);
        brandDataCache.put(brand, newBrandCache);
    }
    
    /**
     * 특정 브랜드의 최근 데이터를 반환합니다.
     */
    public List<JSONObject> getRecentDataForBrand(String brand) {
        LinkedList<JSONObject> brandCache = brandDataCache.getOrDefault(brand, new LinkedList<>());
        return new ArrayList<>(brandCache);
    }
    
    /**
     * 새 데이터를 처리하고 관련 사용자에게 전송합니다.
     */
    public void processNewData(JSONObject salesData) {
        String brand = salesData.getString("store_brand");
        
        // 캐시에 저장
        cacheDataByBrand(salesData);
        
        // 기존 토픽으로도 데이터 전송 (호환성 유지)
        JSONObject summaryData = new JSONObject();
        summaryData.put("franchise_id", salesData.getInt("franchise_id"));
        summaryData.put("store_brand", brand);
        summaryData.put("store_count", salesData.getInt("store_count"));
        summaryData.put("total_sales", salesData.getLong("total_sales"));
        summaryData.put("update_time", salesData.getString("update_time"));
        summaryData.put("event_type", "sales_total_update");
        summaryData.put("server_received_time", LocalDateTime.now().format(formatter));
        
        // 고유 ID 추가
        if (!summaryData.has("id")) {
            summaryData.put("id", System.currentTimeMillis() + "-" + Math.random());
        }
        
        messagingTemplate.convertAndSend("/topic/sales-total", summaryData.toString());
        
        // 해당 브랜드를 선택한 사용자에게만 실시간 업데이트 전송
        userBrandSelections.forEach((userSessionId, selectedBrand) -> {
            if (selectedBrand.equals(brand)) {
                JSONObject updateData = new JSONObject(salesData.toString());
                updateData.put("event_type", "brand_data_update");
                updateData.put("server_received_time", LocalDateTime.now().format(formatter));
                
                // 고유 ID 추가
                if (!updateData.has("id")) {
                    updateData.put("id", System.currentTimeMillis() + "-" + Math.random());
                }
                
                messagingTemplate.convertAndSendToUser(
                    userSessionId, 
                    "/topic/brand-data-update", 
                    updateData.toString()
                );
            }
        });
    }
    
    /**
     * 연결 종료 시 사용자 세션 정리
     */
    public void removeUserSession(String sessionId) {
        userBrandSelections.remove(sessionId);
    }
}
