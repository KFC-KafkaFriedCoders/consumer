// 이 파일은 BrandDataController로 통합되었습니다.
// BrandDataController에서 브랜드 선택 기능을 포함한 모든 WebSocket 매핑을 처리합니다.
// 더 이상 사용하지 않습니다.

/*
package org.example.consumer.controller;

import org.example.consumer.model.BrandSelection;
import org.example.consumer.service.BrandFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class BrandSelectionController {

    @Autowired
    private BrandFilterService brandFilterService;
    
    @MessageMapping("/select-brand")
    public void selectBrand(@Payload BrandSelection selection, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String brand = selection.getBrand();
        
        brandFilterService.registerUserBrandSelection(sessionId, brand);
        
        System.out.println("사용자 세션 " + sessionId + "이(가) 브랜드 '" + brand + "'를 선택했습니다.");
    }
    
    @MessageMapping("/reset-brand-selection") 
    public void resetBrandSelection(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        brandFilterService.clearUserBrandSelection(sessionId);
        
        System.out.println("사용자 세션 " + sessionId + "의 브랜드 선택이 초기화되었습니다.");
    }
}
*/