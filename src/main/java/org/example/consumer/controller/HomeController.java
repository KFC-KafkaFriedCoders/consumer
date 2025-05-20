package org.example.consumer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * 루트 URL 요청을 React 앱의 index.html로 리다이렉트
     */
    @GetMapping("/")
    public String home() {
        return "forward:/build/index.html";
    }
    
    /**
     * SPA 라우팅을 위해 모든 경로를 React 앱의 index.html로 포워드
     * - React Router를 사용하는 경우 필요
     */
    @GetMapping(value = "/{path:[^\\.]*}")
    public String forward() {
        return "forward:/build/index.html";
    }
}
