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
        
        // 브랜드 선택 정보 등록
        brandFilterService.registerUserBrandSelection(sessionId, brand);
        
        System.out.println("사용자 세션 " + sessionId + "이(가) 브랜드 '" + brand + "'를 선택했습니다.");
    }
    
    // 선택을 초기화하는 메서드
    @MessageMapping("/reset-brand-selection") 
    public void resetBrandSelection(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        brandFilterService.clearUserBrandSelection(sessionId);
        
        System.out.println("사용자 세션 " + sessionId + "의 브랜드 선택이 초기화되었습니다.");
    }
}
