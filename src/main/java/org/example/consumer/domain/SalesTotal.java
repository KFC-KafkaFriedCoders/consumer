package org.example.consumer.domain;

import org.json.JSONObject;
import java.util.List;

public class SalesTotal {
    private String id;
    private String storeBrand;
    private double totalSales;
    private int totalTransactions;
    private double averageTransaction;
    private String period;
    private String timestamp;
    private String serverReceivedTime;
    private String eventType;
    
    public SalesTotal() {
    }
    
    public SalesTotal(JSONObject jsonData) {
        this.id = jsonData.optString("id", null);
        this.storeBrand = jsonData.optString("store_brand", "");
        this.totalSales = jsonData.optDouble("total_sales", 0.0);
        this.totalTransactions = jsonData.optInt("total_transactions", 0);
        this.averageTransaction = jsonData.optDouble("average_transaction", 0.0);
        this.period = jsonData.optString("period", "");
        this.timestamp = jsonData.optString("timestamp", "");
        this.serverReceivedTime = jsonData.optString("server_received_time", "");
        this.eventType = jsonData.optString("event_type", "");
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("store_brand", storeBrand);
        json.put("total_sales", totalSales);
        json.put("total_transactions", totalTransactions);
        json.put("average_transaction", averageTransaction);
        json.put("period", period);
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

    public double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public double getAverageTransaction() {
        return averageTransaction;
    }

    public void setAverageTransaction(double averageTransaction) {
        this.averageTransaction = averageTransaction;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
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
