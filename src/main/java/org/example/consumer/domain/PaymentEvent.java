package org.example.consumer.domain;

import org.json.JSONObject;
import java.time.LocalDateTime;

public class PaymentEvent {
    private String id;
    private String storeBrand;
    private String storeId;
    private String storeName;
    private double amount;
    private String paymentMethod;
    private String userId;
    private String timestamp;
    private String serverReceivedTime;
    private String eventType;
    
    public PaymentEvent() {
    }
    
    public PaymentEvent(JSONObject jsonData) {
        this.id = jsonData.optString("id", null);
        this.storeBrand = jsonData.optString("store_brand", "");
        this.storeId = jsonData.optString("store_id", "");
        this.storeName = jsonData.optString("store_name", "");
        this.amount = jsonData.optDouble("amount", 0.0);
        this.paymentMethod = jsonData.optString("payment_method", "");
        this.userId = jsonData.optString("user_id", "");
        this.timestamp = jsonData.optString("timestamp", "");
        this.serverReceivedTime = jsonData.optString("server_received_time", "");
        this.eventType = jsonData.optString("event_type", "");
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("store_brand", storeBrand);
        json.put("store_id", storeId);
        json.put("store_name", storeName);
        json.put("amount", amount);
        json.put("payment_method", paymentMethod);
        json.put("user_id", userId);
        json.put("timestamp", timestamp);
        json.put("server_received_time", serverReceivedTime);
        json.put("event_type", eventType);
        return json;
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoreBrand() {
        return storeBrand;
    }

    public void setStoreBrand(String storeBrand) {
        this.storeBrand = storeBrand;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getServerReceivedTime() {
        return serverReceivedTime;
    }

    public void setServerReceivedTime(String serverReceivedTime) {
        this.serverReceivedTime = serverReceivedTime;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
