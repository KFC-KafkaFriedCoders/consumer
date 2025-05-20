package org.example.consumer.domain;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TopStores {
    private String id;
    private String storeBrand;
    private List<TopStore> topStores;
    private String timestamp;
    private String serverReceivedTime;
    private String eventType;
    
    public TopStores() {
        this.topStores = new ArrayList<>();
    }
    
    public TopStores(JSONObject jsonData) {
        this();
        this.id = jsonData.optString("id", null);
        this.storeBrand = jsonData.optString("store_brand", "");
        this.timestamp = jsonData.optString("timestamp", "");
        this.serverReceivedTime = jsonData.optString("server_received_time", "");
        this.eventType = jsonData.optString("event_type", "");
        
        if (jsonData.has("top_stores")) {
            JSONArray topStoresArray = jsonData.getJSONArray("top_stores");
            for (int i = 0; i < topStoresArray.length(); i++) {
                JSONObject storeJson = topStoresArray.getJSONObject(i);
                TopStore store = new TopStore(storeJson);
                this.topStores.add(store);
            }
        }
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("store_brand", storeBrand);
        json.put("timestamp", timestamp);
        json.put("server_received_time", serverReceivedTime);
        json.put("event_type", eventType);
        
        JSONArray topStoresArray = new JSONArray();
        for (TopStore store : topStores) {
            topStoresArray.put(store.toJson());
        }
        json.put("top_stores", topStoresArray);
        
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

    public List<TopStore> getTopStores() {
        return topStores;
    }

    public void setTopStores(List<TopStore> topStores) {
        this.topStores = topStores;
    }
    
    public void addTopStore(TopStore store) {
        this.topStores.add(store);
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
    
    public static class TopStore {
        private String storeId;
        private String storeName;
        private double salesAmount;
        private int transactionCount;
        
        public TopStore() {
        }
        
        public TopStore(JSONObject jsonData) {
            this.storeId = jsonData.optString("store_id", "");
            this.storeName = jsonData.optString("store_name", "");
            this.salesAmount = jsonData.optDouble("sales_amount", 0.0);
            this.transactionCount = jsonData.optInt("transaction_count", 0);
        }
        
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("store_id", storeId);
            json.put("store_name", storeName);
            json.put("sales_amount", salesAmount);
            json.put("transaction_count", transactionCount);
            return json;
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

        public double getSalesAmount() {
            return salesAmount;
        }

        public void setSalesAmount(double salesAmount) {
            this.salesAmount = salesAmount;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
        }
    }
}
