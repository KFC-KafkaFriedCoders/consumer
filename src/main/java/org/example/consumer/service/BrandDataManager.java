package org.example.consumer.service;

import org.example.consumer.config.properties.BrandProperties;
import org.example.consumer.config.properties.DataProperties;
import org.example.consumer.service.interfaces.BrandDataService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrandDataManager implements BrandDataService {
    private static final Logger log = LoggerFactory.getLogger(BrandDataManager.class);
    
    private final List<String> brands;
    private final int maxMessagesPerBrand;
    
    // 브랜드별 데이터 저장소
    private final Map<String, LinkedList<JSONObject>> paymentLimitData = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<JSONObject>> sameUserData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> salesTotalData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> topStoresData = new ConcurrentHashMap<>();
    
    @Autowired
    public BrandDataManager(BrandProperties brandProperties, DataProperties dataProperties) {
        this.brands = brandProperties.getNamesList();
        this.maxMessagesPerBrand = dataProperties.getMaxMessagesPerBrand();
        
        // 모든 브랜드에 대해 초기 빈 리스트 생성
        for (String brand : brands) {
            paymentLimitData.put(brand, new LinkedList<>());
            sameUserData.put(brand, new LinkedList<>());
        }
        log.info("BrandDataManager 초기화 완료. 관리 브랜드 수: {}", brands.size());
    }
    
    // 결제 한도 데이터 추가
    @Async("taskExecutor")
    public void addPaymentLimitData(JSONObject data) {
        try {
            if (data == null) {
                log.warn("결제 한도 데이터가 null입니다.");
                return;
            }
            
            String brand = data.optString("store_brand", "");
            if (brands.contains(brand)) {
                LinkedList<JSONObject> brandData = paymentLimitData.get(brand);
                synchronized (brandData) {
                    brandData.addFirst(data);
                    // 최대 개수 초과 시 오래된 데이터 제거
                    while (brandData.size() > maxMessagesPerBrand) {
                        brandData.removeLast();
                    }
                }
                log.debug("결제 한도 데이터 추가 - 브랜드: {}, 현재 개수: {}", brand, brandData.size());
            } else {
                log.warn("알 수 없는 브랜드의 결제 한도 데이터: '{}'", brand);
            }
        } catch (Exception e) {
            log.error("결제 한도 데이터 추가 중 오류: {}", e.getMessage());
        }
    }
    
    // 동일인 결제 데이터 추가
    @Async("taskExecutor")
    public void addSameUserData(JSONObject data) {
        try {
            if (data == null) {
                log.warn("동일인 결제 데이터가 null입니다.");
                return;
            }
            
            String brand = data.optString("store_brand", "");
            if (brands.contains(brand)) {
                LinkedList<JSONObject> brandData = sameUserData.get(brand);
                synchronized (brandData) {
                    brandData.addFirst(data);
                    // 최대 개수 초과 시 오래된 데이터 제거
                    while (brandData.size() > maxMessagesPerBrand) {
                        brandData.removeLast();
                    }
                }
                log.debug("동일인 결제 데이터 추가 - 브랜드: {}, 현재 개수: {}", brand, brandData.size());
            } else {
                log.warn("알 수 없는 브랜드의 동일인 결제 데이터: '{}'", brand);
            }
        } catch (Exception e) {
            log.error("동일인 결제 데이터 추가 중 오류: {}", e.getMessage());
        }
    }
    
    // 매출 총합 데이터 업데이트
    @Async("taskExecutor")
    public void updateSalesTotalData(JSONObject data) {
        try {
            if (data == null) {
                log.warn("매출 총합 데이터가 null입니다.");
                return;
            }
            
            String brand = data.optString("store_brand", "");
            if (brands.contains(brand)) {
                salesTotalData.put(brand, data);
                log.debug("매출 총합 데이터 업데이트 - 브랜드: {}", brand);
            } else {
                log.warn("알 수 없는 브랜드의 매출 총합 데이터: '{}'", brand);
            }
        } catch (Exception e) {
            log.error("매출 총합 데이터 업데이트 중 오류: {}", e.getMessage());
        }
    }
    
    // Top Stores 데이터 업데이트 (브랜드 추출 로직 개선)
    @Async("taskExecutor")
    public void updateTopStoresData(JSONObject data) {
        try {
            if (data == null) {
                log.warn("Top Stores 데이터가 null입니다.");
                return;
            }
            
            // top_stores 배열 확인
            if (!data.has("top_stores") || data.getJSONArray("top_stores").length() == 0) {
                log.warn("Top Stores 데이터에 유효한 top_stores 배열이 없습니다.");
                return;
            }
            
            String extractedBrand = extractBrandFromTopStores(data);
            
            if (extractedBrand != null && brands.contains(extractedBrand)) {
                // 데이터에 브랜드 정보 추가
                data.put("store_brand", extractedBrand);
                topStoresData.put(extractedBrand, data);
                log.debug("Top Stores 데이터 업데이트 - 브랜드: {}", extractedBrand);
            } else {
                log.warn("알 수 없는 브랜드의 Top Stores 데이터: '{}'", extractedBrand);
            }
        } catch (Exception e) {
            log.error("Top Stores 데이터 업데이트 중 오류: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Top Stores 데이터에서 브랜드 추출
    private String extractBrandFromTopStores(JSONObject data) {
        try {
            // 1. 직접 store_brand 필드 확인
            String brand = data.optString("store_brand", "");
            if (!brand.isEmpty() && !brand.equals("Unknown")) {
                return brand;
            }
            
            // 2. top_stores 배열에서 첫 번째 매장의 브랜드 추출
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
            
            // 3. franchise_id를 통한 브랜드 매핑 시도 (향후 확장 가능)
            if (data.has("franchise_id")) {
                int franchiseId = data.getInt("franchise_id");
                String mappedBrand = mapFranchiseIdToBrand(franchiseId);
                if (mappedBrand != null) {
                    return mappedBrand;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("브랜드 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }
    
    // franchise_id와 브랜드 매핑 (향후 확장 가능)
    private String mapFranchiseIdToBrand(int franchiseId) {
        // 현재는 매핑 테이블이 없으므로 null 반환
        // 필요시 franchise_id와 브랜드의 매핑 테이블을 여기에 구현
        return null;
    }
    
    // 브랜드별 결제 한도 데이터 조회
    public List<JSONObject> getPaymentLimitData(String brand) {
        LinkedList<JSONObject> brandData = paymentLimitData.get(brand);
        if (brandData != null) {
            synchronized (brandData) {
                return new ArrayList<>(brandData);
            }
        }
        return new ArrayList<>();
    }
    
    // 브랜드별 동일인 결제 데이터 조회
    public List<JSONObject> getSameUserData(String brand) {
        LinkedList<JSONObject> brandData = sameUserData.get(brand);
        if (brandData != null) {
            synchronized (brandData) {
                return new ArrayList<>(brandData);
            }
        }
        return new ArrayList<>();
    }
    
    // 브랜드별 매출 총합 데이터 조회
    public JSONObject getSalesTotalData(String brand) {
        return salesTotalData.get(brand);
    }
    
    // 브랜드별 Top Stores 데이터 조회
    public JSONObject getTopStoresData(String brand) {
        return topStoresData.get(brand);
    }
    
    // 전체 브랜드 목록 조회
    public List<String> getAllBrands() {
        return new ArrayList<>(brands);
    }
    
    // 브랜드가 유효한지 확인
    public boolean isValidBrand(String brand) {
        return brands.contains(brand);
    }
    
    // 현재 저장된 데이터 상태 출력 (디버깅용)
    public void printCurrentStatus() {
        StringBuilder status = new StringBuilder("=== BrandDataManager 현재 상태 ===\n");
        for (String brand : brands) {
            int paymentLimitCount = paymentLimitData.get(brand).size();
            int sameUserCount = sameUserData.get(brand).size();
            boolean hasSalesTotal = salesTotalData.containsKey(brand);
            boolean hasTopStores = topStoresData.containsKey(brand);
            
            status.append(String.format("브랜드: %s | 결제한도: %d개 | 동일인: %d개 | 매출총합: %s | Top매장: %s\n", 
                brand, paymentLimitCount, sameUserCount, 
                hasSalesTotal ? "있음" : "없음", hasTopStores ? "있음" : "없음"));
        }
        status.append("================================");
        log.info(status.toString());
    }
    
    // 특정 브랜드의 모든 데이터 삭제 (필요시 사용)
    @Async("taskExecutor")
    public void clearBrandData(String brand) {
        if (brands.contains(brand)) {
            paymentLimitData.get(brand).clear();
            sameUserData.get(brand).clear();
            salesTotalData.remove(brand);
            topStoresData.remove(brand);
            log.info("브랜드 데이터 삭제 완료: {}", brand);
        }
    }
    
    // 모든 브랜드 데이터 삭제 (필요시 사용)
    @Async("taskExecutor")
    public void clearAllData() {
        for (String brand : brands) {
            clearBrandData(brand);
        }
        log.info("모든 브랜드 데이터 삭제 완료");
    }
}
