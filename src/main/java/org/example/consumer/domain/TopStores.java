package org.example.consumer.domain;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TopStores {
    private String id;
    private int franchiseId;
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
        this.franchiseId = jsonData.optInt("franchise_id", 0);
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
                
                // 최초 추가 시 storeBrand가 비어있으면 첫 번째 매장의 브랜드로 설정
                if (this.storeBrand == null || this.storeBrand.isEmpty()) {
                    this.storeBrand = store.getStoreBrand();
                }
            }
        }
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("franchise_id", franchiseId);
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
    
    public int getFranchiseId() {
        return franchiseId;
    }
    
    public void setFranchiseId(int franchiseId) {
        this.franchiseId = franchiseId;
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
        private String storeBrand;
        private String storeAddress;
        private double totalSales;
        private int rank;
        
        public TopStore() {
        }
        
        public TopStore(JSONObject jsonData) {
            this.storeId = jsonData.optString("store_id", "");
            this.storeName = jsonData.optString("store_name", "");
            this.storeBrand = jsonData.optString("store_brand", "");
            this.storeAddress = jsonData.optString("store_address", "");
            this.totalSales = jsonData.optDouble("total_sales", 0.0);
            this.rank = jsonData.optInt("rank", 0);
        }
        
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("store_id", storeId);
            json.put("store_name", storeName);
            json.put("store_brand", storeBrand);
            json.put("store_address", storeAddress);
            json.put("total_sales", totalSales);
            json.put("rank", rank);
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
        
        public String getStoreBrand() {
            return storeBrand;
        }
        
        public void setStoreBrand(String storeBrand) {
            this.storeBrand = storeBrand;
        }
        
        public String getStoreAddress() {
            return storeAddress;
        }
        
        public void setStoreAddress(String storeAddress) {
            this.storeAddress = storeAddress;
        }

        public double getTotalSales() {
            return totalSales;
        }

        public void setTotalSales(double totalSales) {
            this.totalSales = totalSales;
        }
        
        public int getRank() {
            return rank;
        }
        
        public void setRank(int rank) {
            this.rank = rank;
        }
    }
}
