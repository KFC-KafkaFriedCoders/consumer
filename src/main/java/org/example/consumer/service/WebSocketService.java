package org.example.consumer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Messaging Template 게터
     */
    public SimpMessagingTemplate getMessagingTemplate() {
        return messagingTemplate;
    }

    /**
     * 결제 한도 알림을 WebSocket을 통해 클라이언트에게 전송합니다.
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
        
        // 메시지 전송
        messagingTemplate.convertAndSend("/topic/payment-limit", paymentData.toString());
        System.out.println("WebSocket으로 결제 한도 알림 전송: " + paymentData.toString());
    }
    
    /**
     * 동일 사용자 결제 알림을 WebSocket을 통해 클라이언트에게 전송합니다.
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
        
        // 메시지 전송
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
     * 총 매출 데이터를 WebSocket을 통해 클라이언트에게 전송합니다.
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
        
        // 메시지 전송
        messagingTemplate.convertAndSend("/topic/sales-total", salesData.toString());
        System.out.println("WebSocket으로 매출 데이터 전송: " + salesData.toString());
    }
    
    /**
     * Top Stores 데이터를 WebSocket을 통해 클라이언트에게 전송합니다.
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
        
        // 메시지 전송
        messagingTemplate.convertAndSend("/topic/top-stores", topStoresData.toString());
        System.out.println("WebSocket으로 Top Stores 데이터 전송: " + topStoresData.toString());
    }
}
