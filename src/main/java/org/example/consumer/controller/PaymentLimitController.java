package org.example.consumer.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class PaymentLimitController {

    /**
     * 클라이언트로부터 메시지를 받아 처리하는 메서드입니다.
     * 클라이언트가 '/app/request-payment-data'로 메시지를 보내면 이 메서드가 호출됩니다.
     * 
     * @param message 클라이언트로부터 받은 메시지
     * @return 클라이언트에게 전송할 응답 메시지
     */
    @MessageMapping("/request-payment-data")
    @SendTo("/topic/payment-status")
    public String handlePaymentDataRequest(String message) {
        // 여기서는 간단한 확인 메시지만 반환합니다
        return "결제 데이터 요청이 처리되었습니다. 이제 '/topic/payment-limit' 주제를 통해 알림을 받게 됩니다.";
    }
}
