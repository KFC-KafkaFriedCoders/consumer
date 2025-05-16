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
     * 결제 한도 알림을 WebSocket을 통해 클라이언트에게 전송합니다.
     * 
     * @param paymentData 결제 데이터를 담은 JSON 객체
     */
    public void sendPaymentLimitAlert(JSONObject paymentData) {
        // 서버 수신 시간 추가
        paymentData.put("server_received_time", LocalDateTime.now().format(formatter));
        
        // 이벤트 타입 추가 (React 클라이언트에서 이벤트 타입별 처리를 위함)
        paymentData.put("event_type", "payment_limit_alert");
        
        // 메시지 전송
        messagingTemplate.convertAndSend("/topic/payment-limit", paymentData.toString());
        System.out.println("WebSocket으로 결제 한도 알림 전송: " + paymentData.toString());
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
}
