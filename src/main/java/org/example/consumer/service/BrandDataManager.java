package org.example.consumer.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrandDataManager {
    
    private static final List<String> BRANDS = Arrays.asList(
        "빽다방", "한신포차", "돌배기집", "롤링파스타", "리춘시장", "막이오름", 
        "미정국수", "미정국수0410", "백스비어", "백철판0410", "본가", "빽보이피자", "새마을식당", 
        "성성식당", "역전우동0410", "연돈볼카츠", "원조쌈밥집", "인생설렁탕", 
        "제순식당", "홍콩반점0410", "홍콩분식", "고투웍", "대한국밥"
    );
    
    private static final int MAX_MESSAGES_PER_BRAND = 15;
    
    private final Map<String, LinkedList<JSONObject>> paymentLimitData = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<JSONObject>> sameUserData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> salesTotalData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> topStoresData = new ConcurrentHashMap<>();
    
    public BrandDataManager() {
        for (String brand : BRANDS) {
            paymentLimitData.put(brand, new LinkedList<>());
            sameUserData.put(brand, new LinkedList<>());
        }
    }
    
    public void addPaymentLimitData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (BRANDS.contains(brand)) {
            LinkedList<JSONObject> brandData = paymentLimitData.get(brand);
            synchronized (brandData) {
                brandData.addFirst(data);
                while (brandData.size() > MAX_MESSAGES_PER_BRAND) {
                    brandData.removeLast();
                }
            }
        }
    }
    
    public void addSameUserData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (BRANDS.contains(brand)) {
            LinkedList<JSONObject> brandData = sameUserData.get(brand);
            synchronized (brandData) {
                brandData.addFirst(data);
                while (brandData.size() > MAX_MESSAGES_PER_BRAND) {
                    brandData.removeLast();
                }
            }
        }
    }
    
    public void updateSalesTotalData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (BRANDS.contains(brand)) {
            salesTotalData.put(brand, data);
        }
    }
    
    public void updateTopStoresData(JSONObject data) {
        String extractedBrand = extractBrandFromTopStores(data);
        
        if (extractedBrand != null && BRANDS.contains(extractedBrand)) {
            data.put("store_brand", extractedBrand);
            topStoresData.put(extractedBrand, data);
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
        return new ArrayList<>(BRANDS);
    }
    
    public boolean isValidBrand(String brand) {
        return BRANDS.contains(brand);
    }
    
    public void clearBrandData(String brand) {
        if (BRANDS.contains(brand)) {
            paymentLimitData.get(brand).clear();
            sameUserData.get(brand).clear();
            salesTotalData.remove(brand);
            topStoresData.remove(brand);
        }
    }
    
    public void clearAllData() {
        for (String brand : BRANDS) {
            clearBrandData(brand);
        }
    }
}