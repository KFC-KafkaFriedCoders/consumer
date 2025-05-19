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
public class TopStoresFilterService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 사용자 세션별 브랜드 선택 저장
    private final Map<String, String> userBrandSelections = new ConcurrentHashMap<>();
    
    // 브랜드별 최신 Top Stores 데이터 캐싱 (각 브랜드별로 1개씩만 저장)
    private final Map<String, JSONObject> brandTopStoresCache = new ConcurrentHashMap<>();
    
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
        
        // 해당 브랜드의 캐시된 Top Stores 데이터 전송
        sendCachedTopStoresData(sessionId, brand);
    }
    
    /**
     * 브랜드 선택 시 해당 브랜드의 캐시된 Top Stores 데이터 전송
     */
    public void sendCachedTopStoresData(String sessionId, String brand) {
        JSONObject cachedData = brandTopStoresCache.get(brand);
        
        if (cachedData != null) {
            JSONObject batchData = new JSONObject();
            batchData.put("event_type", "top_stores_data_batch");
            batchData.put("brand", brand);
            batchData.put("data", cachedData);
            batchData.put("time", LocalDateTime.now().format(formatter));
            
            // 사용자에게 해당 브랜드의 캐시된 Top Stores 데이터 전송
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/topic/top-stores-data", 
                batchData.toString()
            );
            
            System.out.println("브랜드 " + brand + "의 캐시된 Top Stores 데이터 전송");
        } else {
            // 데이터가 없을 경우 빈 데이터셋 전송
            JSONObject emptyData = new JSONObject();
            emptyData.put("event_type", "top_stores_data_empty");
            emptyData.put("brand", brand);
            emptyData.put("message", "No top stores data available for this brand");
            
            messagingTemplate.convertAndSendToUser(
                sessionId, 
                "/topic/top-stores-data", 
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
     * 브랜드별 Top Stores 데이터를 캐시에 저장합니다. (최신 데이터 1개만 유지)
     */
    public void cacheTopStoresDataByBrand(JSONObject topStoresData) {
        // top_stores 배열에서 첫 번째 매장의 브랜드를 추출
        JSONArray topStores = topStoresData.getJSONArray("top_stores");
        if (topStores.length() > 0) {
            String brand = topStores.getJSONObject(0).getString("store_brand");
            
            // 데이터에 고유 ID 추가 (클라이언트에서 NEW 배지 컴포넌트용)
            JSONObject dataWithId = new JSONObject(topStoresData.toString());
            if (!dataWithId.has("id")) {
                dataWithId.put("id", System.currentTimeMillis() + "-" + Math.random());
            }
            
            // 각 브랜드별로 최신 Top Stores 데이터 1개만 저장
            brandTopStoresCache.put(brand, dataWithId);
        }
    }
    
    /**
     * 특정 브랜드의 Top Stores 데이터를 반환합니다.
     */
    public JSONObject getTopStoresDataForBrand(String brand) {
        return brandTopStoresCache.get(brand);
    }
    
    /**
     * 새 Top Stores 데이터를 처리하고 관련 사용자에게 전송합니다.
     */
    public void processNewData(JSONObject topStoresData) {
        // top_stores 배열에서 첫 번째 매장의 브랜드를 추출
        JSONArray topStores = topStoresData.getJSONArray("top_stores");
        if (topStores.length() == 0) {
            System.out.println("Top stores 배열이 비어있습니다.");
            return;
        }
        
        String brand = topStores.getJSONObject(0).getString("store_brand");
        
        // 캐시에 저장
        cacheTopStoresDataByBrand(topStoresData);
        
        // 기존 토픽으로 완전한 데이터 전송 (요약이 아닌 전체 데이터)
        JSONObject completeData = new JSONObject(topStoresData.toString());
        completeData.put("store_brand", brand); // 브랜드 정보 추가
        completeData.put("event_type", "top_stores_update");
        completeData.put("server_received_time", LocalDateTime.now().format(formatter));
        
        // 고유 ID 추가
        if (!completeData.has("id")) {
            completeData.put("id", System.currentTimeMillis() + "-" + Math.random());
        }
        
        // 완전한 TOP Stores 데이터를 기본 토픽으로 전송
        messagingTemplate.convertAndSend("/topic/top-stores", completeData.toString());
        
        // 해당 브랜드를 선택한 사용자에게만 실시간 업데이트 전송
        userBrandSelections.forEach((userSessionId, selectedBrand) -> {
            if (selectedBrand.equals(brand)) {
                JSONObject updateData = new JSONObject(topStoresData.toString());
                updateData.put("event_type", "top_stores_data_update");
                updateData.put("server_received_time", LocalDateTime.now().format(formatter));
                
                // 고유 ID 추가
                if (!updateData.has("id")) {
                    updateData.put("id", System.currentTimeMillis() + "-" + Math.random());
                }
                
                messagingTemplate.convertAndSendToUser(
                    userSessionId, 
                    "/topic/top-stores-data-update", 
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
