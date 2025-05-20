package org.example.consumer.service.interfaces;

import org.json.JSONObject;

public interface WebSocketNotificationService {
    void sendPaymentLimitAlert(JSONObject data);
    
    void sendSameUserAlert(JSONObject data);
    
    void sendSalesTotalData(JSONObject data);
    
    void sendTopStoresData(JSONObject data);
    
    void sendServerStatus(String status);
    
    void sendBrandPaymentLimitData(String brand, String sessionId);
    
    void sendBrandSameUserData(String brand, String sessionId);
    
    void sendBrandSalesTotalData(String brand, String sessionId);
    
    void sendBrandTopStoresData(String brand, String sessionId);
    
    void sendAllBrandData(String sessionId);
}
