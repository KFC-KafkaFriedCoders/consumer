package org.example.consumer.config;

import org.example.consumer.service.BrandFilterService;
import org.example.consumer.service.interfaces.WebSocketNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final BrandFilterService brandFilterService;
    private final WebSocketNotificationService webSocketService;
    
    @Autowired
    public WebSocketEventListener(BrandFilterService brandFilterService,
                                WebSocketNotificationService webSocketService) {
        this.brandFilterService = brandFilterService;
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void handleWebSocketConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("사용자 세션 연결: {}", sessionId);
        
        // 연결 성공 상태 메시지 전송
        webSocketService.sendServerStatus("connected");
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 연결 종료 시 세션 정리
        brandFilterService.removeUserSession(sessionId);
        log.info("사용자 세션 종료: {}", sessionId);
    }
}
