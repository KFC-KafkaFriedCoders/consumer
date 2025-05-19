package org.example.consumer.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    
    /**
     * Messaging Template 게터
     */
    public SimpMessagingTemplate getMessagingTemplate() {
        return messagingTemplate;
    }

    /**
     * 결제 한도 알림을 WebSocket을 통해 클라이언트에게 전송하고 브랜드별 데이터로 저장합니다.
     * 
     * @param paymentData 결제 데이터를 담은 JSON 객체
     */
    public void sendPaymentLimitAlert(JSONObject paymentData) {
        // 서버 수신 시간 추가
        paymentData.put("server_received_time", LocalDateTime.now().format(formatter));
        
        // 이벤트 타입 추가 (React 클라이언트에서 이벤트 타입별 처리를 위함)
        paymentData.put("event_type", "payment_limit_alert");
        
        // 고유 ID 추가 (NEW 배지 기능을 위해)
        if (!paymentData.has("id")) {
            paymentData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        // 브랜드별 데이터로 저장
        brandDataManager.addPaymentLimitData(paymentData);
        
        // 메시지 전송 (모든 클라이언트에게)
        messagingTemplate.convertAndSend("/topic/payment-limit", paymentData.toString());
        System.out.println("WebSocket으로 결제 한도 알림 전송: " + paymentData.toString());
    }
    
    /**
     * 동일 사용자 결제 알림을 WebSocket을 통해 클라이언트에게 전송하고 브랜드별 데이터로 저장합니다.
     * 
     * @param sameUserData 동일 사용자 결제 데이터를 담은 JSON 객체
     */
    public void sendSameUserAlert(JSONObject sameUserData) {
        // 서버 수신 시간 추가
        sameUserData.put("server_received_time", LocalDateTime.now().format(formatter));
        
        // 이벤트 타입 추가 (React 클라이언트에서 이벤트 타입별 처리를 위함)
        sameUserData.put("event_type", "payment_same_user_alert");
        
        // 고유 ID 추가 (NEW 배지 기능을 위해)
        if (!sameUserData.has("id")) {
            sameUserData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        // 브랜드별 데이터로 저장
        brandDataManager.addSameUserData(sameUserData);
        
        // 메시지 전송 (모든 클라이언트에게)
        messagingTemplate.convertAndSend("/topic/payment-same-user", sameUserData.toString());
        System.out.println("WebSocket으로 동일 사용자 결제 알림 전송: " + sameUserData.toString());
    }
    
    /**
     * 서버 상태 메시지를 클라이언트에게 전송합니다.
     */
    public void sendServerStatus(String status) {
        JSONObject statusData = new JSONObject();
        statusData.put("event_type", "server_status");
        statusData.put("status", status);
        statusData.put("time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSend("/topic/server-status", statusData.toString());
    }
    
    /**
     * 총 매출 데이터를 WebSocket을 통해 클라이언트에게 전송하고 브랜드별 데이터로 저장합니다.
     * 
     * @param salesData 매출 데이터를 담은 JSON 객체
     */
    public void sendSalesTotalData(JSONObject salesData) {
        // 서버 수신 시간 추가
        salesData.put("server_received_time", LocalDateTime.now().format(formatter));
        
        // 이벤트 타입 추가 (React 클라이언트에서 이벤트 타입별 처리를 위함)
        salesData.put("event_type", "sales_total_update");
        
        // 고유 ID 추가 (NEW 배지 기능을 위해)
        if (!salesData.has("id")) {
            salesData.put("id", System.currentTimeMillis() + Math.random());
        }
        
        // 브랜드별 데이터로 저장
        brandDataManager.updateSalesTotalData(salesData);
        
        // 메시지 전송 (모든 클라이언트에게)
        messagingTemplate.convertAndSend("/topic/sales-total", salesData.toString());
        System.out.println("WebSocket으로 매출 데이터 전송: " + salesData.toString());
    }
    
    /**
     * Top Stores 데이터를 WebSocket을 통해 클라이언트에게 전송하고 브랜드별 데이터로 저장합니다.
     * 
     * @param topStoresData Top Stores 데이터를 담은 JSON 객체
     */
    public void sendTopStoresData(JSONObject topStoresData) {
        // 서버 수신 시간 추가
        topStoresData.put("server_received_time", LocalDateTime.now().format(formatter));
        
        // 이벤트 타입 추가 (React 클라이언트에서 이벤트 타입별 처리를 위함)
        topStoresData.put("event_type", "top_stores_update");
        
        // 고유 ID 추가 (NEW 배지 기능을 위해)
        if (!topStoresData.has("id")) {
            topStoresData.put("id", System.currentTimeMillis() + "-" + Math.random());
        }
        
        // 브랜드별 데이터로 저장
        brandDataManager.updateTopStoresData(topStoresData);
        
        // 메시지 전송 (모든 클라이언트에게)
        messagingTemplate.convertAndSend("/topic/top-stores", topStoresData.toString());
        System.out.println("WebSocket으로 Top Stores 데이터 전송: " + topStoresData.toString());
    }
    
    // ===== 브랜드별 데이터 요청 응답 메서드들 =====
    
    /**
     * 브랜드별 결제 한도 데이터를 특정 클라이언트에게 전송합니다.
     * 
     * @param brand 요청할 브랜드
     * @param sessionId 세션 ID (개별 클라이언트에게 전송하기 위함)
     */
    public void sendBrandPaymentLimitData(String brand, String sessionId) {
        List<JSONObject> brandData = brandDataManager.getPaymentLimitData(brand);
        
        JSONObject response = new JSONObject();
        response.put("event_type", "brand_payment_limit_data");
        response.put("brand", brand);
        
        // JSONObject 리스트를 JSONArray로 변환
        JSONArray dataArray = new JSONArray();
        for (JSONObject data : brandData) {
            dataArray.put(data);
        }
        response.put("data", dataArray);
        response.put("count", brandData.size());
        response.put("server_sent_time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-payment-limit", response.toString());
        System.out.println("브랜드별 결제 한도 데이터 전송 - 브랜드: " + brand + ", 데이터 수: " + brandData.size());
    }
    
    /**
     * 브랜드별 동일인 결제 데이터를 특정 클라이언트에게 전송합니다.
     * 
     * @param brand 요청할 브랜드
     * @param sessionId 세션 ID (개별 클라이언트에게 전송하기 위함)
     */
    public void sendBrandSameUserData(String brand, String sessionId) {
        List<JSONObject> brandData = brandDataManager.getSameUserData(brand);
        
        JSONObject response = new JSONObject();
        response.put("event_type", "brand_same_user_data");
        response.put("brand", brand);
        
        // JSONObject 리스트를 JSONArray로 변환
        JSONArray dataArray = new JSONArray();
        for (JSONObject data : brandData) {
            dataArray.put(data);
        }
        response.put("data", dataArray);
        response.put("count", brandData.size());
        response.put("server_sent_time", LocalDateTime.now().format(formatter));
        
        messagingTemplate.convertAndSendToUser(sessionId, "/topic/brand-same-user", response.toString());
        System.out.println("브랜드별 동일인 결제 데이터 전송 - 브랜드: " + brand + ", 데이터 수: " + brandData.size());
    }
    
    /**
     * 브랜드별 매출 총합 데이터를 특정 클라이언트에게 전송합니다.
     * 
     * @param brand 요청할 브랜드
     * @param sessionId 세션 ID (개별 클라이언트에게 전송하기 위함)
     */
    public void sendBrandSalesTotalData(String brand, String sessionId) {
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
        System.out.println("브랜드별 매출 총합 데이터 전송 - 브랜드: " + brand + ", 데이터 존재: " + (brandData != null));
    }
    
    /**
     * 브랜드별 Top Stores 데이터를 특정 클라이언트에게 전송합니다.
     * 
     * @param brand 요청할 브랜드
     * @param sessionId 세션 ID (개별 클라이언트에게 전송하기 위함)
     */
    public void sendBrandTopStoresData(String brand, String sessionId) {
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
        System.out.println("브랜드별 Top Stores 데이터 전송 - 브랜드: " + brand + ", 데이터 존재: " + (brandData != null));
    }
    
    /**
     * 모든 브랜드 목록을 특정 클라이언트에게 전송합니다.
     * 
     * @param sessionId 세션 ID (개별 클라이언트에게 전송하기 위함)
     */
    public void sendAllBrandData(String sessionId) {
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
        System.out.println("모든 브랜드 목록 전송 - 브랜드 수: " + brands.size());
    }
}
