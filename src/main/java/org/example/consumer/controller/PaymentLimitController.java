// 이 파일은 BrandDataController로 통합되었습니다.
// BrandDataController에서 모든 WebSocket 매핑을 처리합니다.
// 더 이상 사용하지 않습니다.

/*
package org.example.consumer.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class PaymentLimitController {

    @MessageMapping("/request-payment-data")
    @SendTo("/topic/payment-status")
    public String handlePaymentDataRequest(String message) {
        return "결제 데이터 요청이 처리되었습니다. 이제 '/topic/payment-limit' 주제를 통해 알림을 받게 됩니다.";
    }
    
    @MessageMapping("/request-same-user-data")
    @SendTo("/topic/same-user-status")
    public String handleSameUserDataRequest(String message) {
        return "동일 사용자 결제 데이터 요청이 처리되었습니다. 이제 '/topic/payment-same-user' 주제를 통해 알림을 받게 됩니다.";
    }
}
*/