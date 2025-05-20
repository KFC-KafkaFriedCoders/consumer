package org.example.consumer.service.interfaces;

import org.json.JSONObject;
import java.util.List;

public interface BrandDataService {
    void addPaymentLimitData(JSONObject data);
    
    void addSameUserData(JSONObject data);
    
    void updateSalesTotalData(JSONObject data);
    
    void updateTopStoresData(JSONObject data);
    
    List<JSONObject> getPaymentLimitData(String brand);
    
    List<JSONObject> getSameUserData(String brand);
    
    JSONObject getSalesTotalData(String brand);
    
    JSONObject getTopStoresData(String brand);
    
    List<String> getAllBrands();
    
    boolean isValidBrand(String brand);
    
    void clearBrandData(String brand);
    
    void clearAllData();
}
