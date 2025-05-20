package org.example.consumer.domain;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SameUserPayment {
    private String id;
    private String storeBrand;
    private String userId;
    private List<String> paymentIds;
    private double totalAmount;
    private int paymentCount;
    private String timestamp;
    private String serverReceivedTime;
    private String eventType;
    
    public SameUserPayment() {
        this.paymentIds = new ArrayList<>();
    }
    
    public SameUserPayment(JSONObject jsonData) {
        this();
        this.id = jsonData.optString("id", null);
        this.storeBrand = jsonData.optString("store_brand", "");
        this.userId = jsonData.optString("user_id", "");
        this.totalAmount = jsonData.optDouble("total_amount", 0.0);
        this.paymentCount = jsonData.optInt("payment_count", 0);
        this.timestamp = jsonData.optString("timestamp", "");
        this.serverReceivedTime = jsonData.optString("server_received_time", "");
        this.eventType = jsonData.optString("event_type", "");
        
        if (jsonData.has("payment_ids")) {
            JSONArray idsArray = jsonData.getJSONArray("payment_ids");
            for (int i = 0; i < idsArray.length(); i++) {
                this.paymentIds.add(idsArray.getString(i));
            }
        }
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("store_brand", storeBrand);
        json.put("user_id", userId);
        json.put("total_amount", totalAmount);
        json.put("payment_count", paymentCount);
        json.put("timestamp", timestamp);
        json.put("server_received_time", serverReceivedTime);
        json.put("event_type", eventType);
        
        JSONArray idsArray = new JSONArray();
        for (String paymentId : paymentIds) {
            idsArray.put(paymentId);
        }
        json.put("payment_ids", idsArray);
        
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getPaymentIds() {
        return paymentIds;
    }

    public void setPaymentIds(List<String> paymentIds) {
        this.paymentIds = paymentIds;
    }
    
    public void addPaymentId(String paymentId) {
        this.paymentIds.add(paymentId);
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
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
