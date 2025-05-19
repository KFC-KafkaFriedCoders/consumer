package org.example.consumer.controller;

import org.example.consumer.service.BrandDataManager;
import org.example.consumer.service.WebSocketService;
import org.example.consumer.service.BrandFilterService;
import org.example.consumer.model.BrandSelection;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class BrandDataController {

    private final WebSocketService webSocketService;
    private final BrandDataManager brandDataManager;
    private final BrandFilterService brandFilterService;

    @Autowired
    public BrandDataController(WebSocketService webSocketService, 
                               BrandDataManager brandDataManager, 
                               BrandFilterService brandFilterService) {
        this.webSocketService = webSocketService;
        this.brandDataManager = brandDataManager;
        this.brandFilterService = brandFilterService;
    }

    // ===== 기존 PaymentLimitController 호환 메서드들 =====
    
    /**
     * 기존 결제 데이터 요청 처리 (호환성 유지)
     */
    @MessageMapping("/request-payment-data")
    @SendTo("/topic/payment-status")
    public String handlePaymentDataRequest(String message) {
        return "결제 데이터 요청이 처리되었습니다. 이제 '/topic/payment-limit' 주제를 통해 알림을 받게 됩니다.";
    }
    
    /**
     * 기존 동일 사용자 데이터 요청 처리 (호환성 유지)
     */
    @MessageMapping("/request-same-user-data")
    @SendTo("/topic/same-user-status")
    public String handleSameUserDataRequest(String message) {
        return "동일 사용자 결제 데이터 요청이 처리되었습니다. 이제 '/topic/payment-same-user' 주제를 통해 알림을 받게 됩니다.";
    }

    // ===== 새로운 브랜드별 데이터 요청 메서드들 =====

    /**
     * 브랜드별 결제 한도 데이터 요청 처리
     */
    @MessageMapping("/brand/request-payment-limit-data")
    public void requestBrandPaymentLimitData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                webSocketService.sendBrandPaymentLimitData(brand, sessionId);
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            System.err.println("결제 한도 데이터 요청 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 브랜드별 동일인 결제 데이터 요청 처리
     */
    @MessageMapping("/brand/request-same-user-data")
    public void requestBrandSameUserData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                webSocketService.sendBrandSameUserData(brand, sessionId);
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            System.err.println("동일인 결제 데이터 요청 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 브랜드별 매출 총합 데이터 요청 처리
     */
    @MessageMapping("/brand/request-sales-total-data")
    public void requestBrandSalesTotalData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                webSocketService.sendBrandSalesTotalData(brand, sessionId);
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            System.err.println("매출 총합 데이터 요청 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 브랜드별 Top Stores 데이터 요청 처리
     */
    @MessageMapping("/brand/request-top-stores-data")
    public void requestBrandTopStoresData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                webSocketService.sendBrandTopStoresData(brand, sessionId);
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            System.err.println("Top Stores 데이터 요청 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 모든 브랜드 목록 요청 처리
     */
    @MessageMapping("/brand/request-all-brands")
    public void requestAllBrands(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            webSocketService.sendAllBrandData(sessionId);
        } catch (Exception e) {
            System.err.println("모든 브랜드 목록 요청 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 브랜드 선택 처리 (JSON 형태)
     */
    @MessageMapping("/select-brand")
    public void selectBrand(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                // 기존 브랜드 필터 서비스에 브랜드 선택 정보 등록
                brandFilterService.registerUserBrandSelection(sessionId, brand);
                
                // 브랜드 선택 시 해당 브랜드의 모든 데이터를 전송
                webSocketService.sendBrandPaymentLimitData(brand, sessionId);
                webSocketService.sendBrandSameUserData(brand, sessionId);
                webSocketService.sendBrandSalesTotalData(brand, sessionId);
                webSocketService.sendBrandTopStoresData(brand, sessionId);
                
                System.out.println("브랜드 선택됨: " + brand + " (세션: " + sessionId + ")");
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            System.err.println("브랜드 선택 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "브랜드 선택 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 브랜드 선택 처리 (BrandSelection 객체 - 호환성 유지)
     */
    @MessageMapping("/select-brand-legacy")
    public void selectBrandLegacy(@Payload BrandSelection selection, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String brand = selection.getBrand();
        
        if (brandDataManager.isValidBrand(brand)) {
            // 브랜드 선택 정보 등록
            brandFilterService.registerUserBrandSelection(sessionId, brand);
            
            // 브랜드 선택 시 해당 브랜드의 모든 데이터를 전송
            webSocketService.sendBrandPaymentLimitData(brand, sessionId);
            webSocketService.sendBrandSameUserData(brand, sessionId);
            webSocketService.sendBrandSalesTotalData(brand, sessionId);
            webSocketService.sendBrandTopStoresData(brand, sessionId);
            
            System.out.println("브랜드 선택됨 (Legacy): " + brand + " (세션: " + sessionId + ")");
        } else {
            sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
        }
    }

    /**
     * 브랜드 선택 초기화
     */
    @MessageMapping("/reset-brand-selection") 
    public void resetBrandSelection(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        brandFilterService.clearUserBrandSelection(sessionId);
        
        System.out.println("사용자 세션 " + sessionId + "의 브랜드 선택이 초기화되었습니다.");
    }

    /**
     * 브랜드별 모든 데이터 일괄 요청 처리
     */
    @MessageMapping("/brand/request-all-data")
    public void requestBrandAllData(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            JSONObject request = new JSONObject(message);
            String brand = request.getString("brand");
            String sessionId = headerAccessor.getSessionId();

            if (brandDataManager.isValidBrand(brand)) {
                // 한 번에 모든 데이터 전송
                webSocketService.sendBrandPaymentLimitData(brand, sessionId);
                webSocketService.sendBrandSameUserData(brand, sessionId);
                webSocketService.sendBrandSalesTotalData(brand, sessionId);
                webSocketService.sendBrandTopStoresData(brand, sessionId);
                
                System.out.println("브랜드 모든 데이터 요청 처리됨: " + brand + " (세션: " + sessionId + ")");
            } else {
                sendErrorResponse(sessionId, "유효하지 않은 브랜드입니다: " + brand);
            }
        } catch (Exception e) {
            System.err.println("브랜드 모든 데이터 요청 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 클라이언트 연결 시 초기 데이터 요청 처리
     */
    @MessageMapping("/init-connection")
    public void initConnection(@Payload String message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            
            // 연결 시 모든 브랜드 목록을 먼저 전송
            webSocketService.sendAllBrandData(sessionId);
            
            // 요청에 기본 브랜드가 포함되어 있으면 해당 브랜드 데이터도 전송
            if (message != null && !message.trim().isEmpty()) {
                try {
                    JSONObject request = new JSONObject(message);
                    if (request.has("defaultBrand")) {
                        String defaultBrand = request.getString("defaultBrand");
                        if (brandDataManager.isValidBrand(defaultBrand)) {
                            // 기존 브랜드 필터 서비스에도 등록
                            brandFilterService.registerUserBrandSelection(sessionId, defaultBrand);
                            
                            // 모든 데이터 전송
                            webSocketService.sendBrandPaymentLimitData(defaultBrand, sessionId);
                            webSocketService.sendBrandSameUserData(defaultBrand, sessionId);
                            webSocketService.sendBrandSalesTotalData(defaultBrand, sessionId);
                            webSocketService.sendBrandTopStoresData(defaultBrand, sessionId);
                        }
                    }
                } catch (Exception e) {
                    // JSON 파싱 실패 시 무시하고 브랜드 목록만 전송
                    System.out.println("초기 연결 메시지 파싱 실패, 브랜드 목록만 전송합니다.");
                }
            }
            
            System.out.println("클라이언트 초기 연결 처리됨 (세션: " + sessionId + ")");
        } catch (Exception e) {
            System.err.println("초기 연결 처리 중 오류: " + e.getMessage());
            sendErrorResponse(headerAccessor.getSessionId(), "초기 연결 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 오류 응답 전송
     */
    private void sendErrorResponse(String sessionId, String errorMessage) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("event_type", "error");
        errorResponse.put("message", errorMessage);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        webSocketService.getMessagingTemplate()
            .convertAndSendToUser(sessionId, "/topic/error", errorResponse.toString());
    }
}
