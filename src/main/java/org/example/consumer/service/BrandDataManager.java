package org.example.consumer.service;

import org.example.consumer.config.BrandConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrandDataManager {
    
    @Autowired
    private BrandConfig brandConfig;
    
    private final Map<String, LinkedList<JSONObject>> paymentLimitData = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<JSONObject>> sameUserData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> salesTotalData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> topStoresData = new ConcurrentHashMap<>();
    
    @Autowired
    public void initializeBrandData() {
        List<String> brands = brandConfig.getNames();
        for (String brand : brands) {
            paymentLimitData.put(brand, new LinkedList<>());
            sameUserData.put(brand, new LinkedList<>());
        }
    }
    
    public void addPaymentLimitData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (isValidBrand(brand)) {
            LinkedList<JSONObject> brandData = paymentLimitData.get(brand);
            synchronized (brandData) {
                brandData.addFirst(data);
                int maxMessages = brandConfig.getMaxMessagesPerBrand();
                while (brandData.size() > maxMessages) {
                    brandData.removeLast();
                }
            }
        }
    }
    
    public void addSameUserData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (isValidBrand(brand)) {
            LinkedList<JSONObject> brandData = sameUserData.get(brand);
            synchronized (brandData) {
                brandData.addFirst(data);
                int maxMessages = brandConfig.getMaxMessagesPerBrand();
                while (brandData.size() > maxMessages) {
                    brandData.removeLast();
                }
            }
        }
    }
    
    public void updateSalesTotalData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (isValidBrand(brand)) {
            salesTotalData.put(brand, data);
        }
    }
    
    public void updateTopStoresData(JSONObject data) {
        String extractedBrand = extractBrandFromTopStores(data);
        
        if (extractedBrand != null && isValidBrand(extractedBrand)) {
            data.put("store_brand", extractedBrand);
            topStoresData.put(extractedBrand, data);
        }
    }
    
    public List<JSONObject> getPaymentLimitData(String brand) {
        LinkedList<JSONObject> brandData = paymentLimitData.get(brand);
        if (brandData != null) {
            synchronized (brandData) {
                return new ArrayList<>(brandData);
            }
        }
        return new ArrayList<>();
    }
    
    public List<JSONObject> getSameUserData(String brand) {
        LinkedList<JSONObject> brandData = sameUserData.get(brand);
        if (brandData != null) {
            synchronized (brandData) {
                return new ArrayList<>(brandData);
            }
        }
        return new ArrayList<>();
    }
    
    public JSONObject getSalesTotalData(String brand) {
        return salesTotalData.get(brand);
    }
    
    public JSONObject getTopStoresData(String brand) {
        return topStoresData.get(brand);
    }
    
    public List<String> getAllBrands() {
        return new ArrayList<>(brandConfig.getNames());
    }
    
    public boolean isValidBrand(String brand) {
        return brandConfig.getNames().contains(brand);
    }
    
    public void clearBrandData(String brand) {
        if (isValidBrand(brand)) {
            paymentLimitData.get(brand).clear();
            sameUserData.get(brand).clear();
            salesTotalData.remove(brand);
            topStoresData.remove(brand);
        }
    }
    
    public void clearAllData() {
        for (String brand : brandConfig.getNames()) {
            clearBrandData(brand);
        }
    }
    
    private String extractBrandFromTopStores(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (!brand.isEmpty() && !brand.equals("Unknown")) {
            return brand;
        }
        
        if (data.has("top_stores")) {
            JSONArray topStores = data.getJSONArray("top_stores");
            if (topStores.length() > 0) {
                JSONObject firstStore = topStores.getJSONObject(0);
                brand = firstStore.optString("store_brand", "");
                if (!brand.isEmpty()) {
                    return brand;
                }
            }
        }
        
        if (data.has("franchise_id")) {
            int franchiseId = data.getInt("franchise_id");
            String mappedBrand = mapFranchiseIdToBrand(franchiseId);
            if (mappedBrand != null) {
                return mappedBrand;
            }
        }
        
        return null;
    }
    
    private String mapFranchiseIdToBrand(int franchiseId) {
        return null;
    }
}
