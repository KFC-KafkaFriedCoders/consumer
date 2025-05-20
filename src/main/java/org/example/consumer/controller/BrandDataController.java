package org.example.consumer.controller;

import org.example.consumer.service.BrandDataManager;
import org.example.consumer.service.WebSocketService;
import org.example.consumer.service.UnifiedBrandFilterService;
import org.example.consumer.model.BrandSelection;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.function.BiConsumer;

@Controller
public class BrandDataController {

    private final WebSocketService webSocketService;
    private final BrandDataManager brandDataManager;
    private final UnifiedBrandFilterService unifiedBrandFilterService;

    @Autowired
    public BrandDataController(WebSocketService webSocketService, 
                               BrandDataManager brandDataManager, 
                               UnifiedBrandFilterService unifiedBrandFilterService) {
        this.webSocketService = webSocketService;
        this.brandDataManager = brandDataManager;
        this.unifiedBrandFilterService = unifiedBrandFilterService;
    }

    // 호환성 유지를 위한 기존 엔드포인트 (브랜드 선택 안내)
    @MessageMapping("/request-payment-data")
    @SendTo("/topic/payment-status")
    public String handlePaymentDataRequest(String message) {
        return "브랜드를 먼저 선택해주세요. 브랜드별 필터링된 데이터만 제공됩니다.";
    }
    
    @MessageMapping("/request-same-user-data")
    @SendTo("/topic/same-user-status")
    public String handleSameUserDataRequest(String message) {
        return "브랜드를 먼저 선택해주세요. 브랜드별 필터링된 데이터만 제공됩니다.";
    }

    // 브랜드별 데이터 요청 (브랜드 선택 필수)
    @MessageMapping("/brand/request-payment-limit-data")
    public void requestBrandPaymentLimitData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        processBrandRequest(message, headerAccessor, webSocketService::sendBrandPaymentLimitData);
    }

    @MessageMapping("/brand/request-same-user-data")
    public void requestBrandSameUserData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        processBrandRequest(message, headerAccessor, webSocketService::sendBrandSameUserData);
    }

    @MessageMapping("/brand/request-sales-total-data")
    public void requestBrandSalesTotalData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        processBrandRequest(message, headerAccessor, webSocketService::sendBrandSalesTotalData);
    }

    @MessageMapping("/brand/request-top-stores-data")
    public void requestBrandTopStoresData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        processBrandRequest(message, headerAccessor, webSocketService::sendBrandTopStoresData);
    }

    // 브랜드 목록 요청
    @MessageMapping("/brand/request-all-brands")
    public void requestAllBrands(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            webSocketService.sendAllBrandData(sessionId);
        } catch (Exception e) {
            sendErrorResponse(headerAccessor.getSessionId(), "브랜드 목록 요청 처리 중 오류가 발생했습니다.");
        }
    }

    // 브랜드 선택 (필수)
    @MessageMapping("/select-brand")
    public void selectBrand(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                System.out.println("[브랜드 선택] 세션: " + sessionId + ", 브랜드: " + brand);
                unifiedBrandFilterService.registerUserBrandSelection(sessionId, brand);
                sendAllBrandData(brand, sessionId);
                sendBrandSelectedConfirmation(sessionId, brand);
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            sendErrorResponse(headerAccessor.getSessionId(), "브랜드 선택 처리 중 오류가 발생했습니다.");
        }
    }

    @MessageMapping("/select-brand-legacy")
    public void selectBrandLegacy(@Payload BrandSelection selection, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String brand = selection.getBrand();
        
        if (brandDataManager.isValidBrand(brand)) {
            System.out.println("[브랜드 선택] 세션: " + sessionId + ", 브랜드: " + brand + " (Legacy)");
            unifiedBrandFilterService.registerUserBrandSelection(sessionId, brand);
            sendAllBrandData(brand, sessionId);
            sendBrandSelectedConfirmation(sessionId, brand);
        } else {
            sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
        }
    }

    // 브랜드 선택 초기화
    @MessageMapping("/reset-brand-selection") 
    public void resetBrandSelection(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            System.out.println("[브랜드 초기화] 세션: " + sessionId);
            unifiedBrandFilterService.clearUserBrandSelection(sessionId);
            sendBrandResetConfirmation(sessionId);
        } catch (Exception e) {
            sendErrorResponse(headerAccessor.getSessionId(), "브랜드 선택 초기화 중 오류가 발생했습니다.");
        }
    }

    // 브랜드별 모든 데이터 요청
    @MessageMapping("/brand/request-all-data")
    public void requestBrandAllData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                sendAllBrandData(brand, sessionId);
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            sendErrorResponse(headerAccessor.getSessionId(), "브랜드 데이터 요청 처리 중 오류가 발생했습니다.");
        }
    }

    // 연결 초기화 (브랜드 목록만 제공)
    @MessageMapping("/init-connection")
    public void initConnection(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            
            // 브랜드 목록만 먼저 전송
            webSocketService.sendAllBrandData(sessionId);
            
            // 브랜드 선택 안내 메시지
            sendBrandSelectionRequired(sessionId);
            
            // 기본 브랜드가 포함되어 있으면 자동 선택
            if (message != null && !message.trim().isEmpty()) {
                try {
                    JSONObject request = new JSONObject(message);
                    if (request.has("defaultBrand")) {
                        String defaultBrand = request.getString("defaultBrand");
                        if (brandDataManager.isValidBrand(defaultBrand)) {
                            unifiedBrandFilterService.registerUserBrandSelection(sessionId, defaultBrand);
                            sendAllBrandData(defaultBrand, sessionId);
                            sendBrandSelectedConfirmation(sessionId, defaultBrand);
                        }
                    }
                } catch (Exception ignored) {
                    // JSON 파싱 실패 시 무시
                }
            }
        } catch (Exception e) {
            sendErrorResponse(headerAccessor.getSessionId(), "초기 연결 처리 중 오류가 발생했습니다.");
        }
    }
    
    // 공통 브랜드 요청 처리
    private void processBrandRequest(String message, SimpMessageHeaderAccessor headerAccessor, BiConsumer<String, String> dataHandler) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                dataHandler.accept(brand, sessionId);
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            sendErrorResponse(headerAccessor.getSessionId(), "요청 처리 중 오류가 발생했습니다.");
        }
    }
    
    // 브랜드별 모든 데이터 전송
    private void sendAllBrandData(String brand, String sessionId) {
        webSocketService.sendBrandPaymentLimitData(brand, sessionId);
        webSocketService.sendBrandSameUserData(brand, sessionId);
        webSocketService.sendBrandSalesTotalData(brand, sessionId);
        webSocketService.sendBrandTopStoresData(brand, sessionId);
    }

    // 브랜드 선택 필수 안내
    private void sendBrandSelectionRequired(String sessionId) {
        JSONObject message = new JSONObject();
        message.put("event_type", "brand_selection_required");
        message.put("message", "브랜드를 선택해야 데이터를 받을 수 있습니다.");
        message.put("timestamp", System.currentTimeMillis());
        
        webSocketService.getMessagingTemplate()
            .convertAndSendToUser(sessionId, "/topic/info", message.toString());
    }

    // 브랜드 선택 완료 확인
    private void sendBrandSelectedConfirmation(String sessionId, String brand) {
        JSONObject message = new JSONObject();
        message.put("event_type", "brand_selected");
        message.put("brand", brand);
        message.put("message", "브랜드가 선택되었습니다: " + brand);
        message.put("timestamp", System.currentTimeMillis());
        
        webSocketService.getMessagingTemplate()
            .convertAndSendToUser(sessionId, "/topic/info", message.toString());
    }

    // 브랜드 선택 초기화 확인
    private void sendBrandResetConfirmation(String sessionId) {
        JSONObject message = new JSONObject();
        message.put("event_type", "brand_reset");
        message.put("message", "브랜드 선택이 초기화되었습니다. 새 브랜드를 선택해주세요.");
        message.put("timestamp", System.currentTimeMillis());
        
        webSocketService.getMessagingTemplate()
            .convertAndSendToUser(sessionId, "/topic/info", message.toString());
    }

    // 에러 응답
    private void sendErrorResponse(String sessionId, String errorMessage) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("event_type", "error");
        errorResponse.put("message", errorMessage);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        webSocketService.getMessagingTemplate()
            .convertAndSendToUser(sessionId, "/topic/error", errorResponse.toString());
    }
}
