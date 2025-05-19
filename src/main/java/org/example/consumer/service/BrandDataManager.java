package org.example.consumer.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrandDataManager {
    
    // 브랜드 목록 (Kafka 데이터와 일치하도록 수정)
    private static final List<String> BRANDS = Arrays.asList(
        "빽다방", "한신포차", "돌배기집", "롤링파스타", "리춘시장", "막이오름", 
        "미정국수", "미정국수0410", "백스비어", "백철판0410", "본가", "빽보이피자", "새마을식당", 
        "성성식당", "역전우동0410", "연돈볼카츠", "원조쌈밥집", "인생설렁탕", 
        "제순식당", "홍콩반점0410", "홍콩분식", "고투웍", "대한국밥"
    );
    
    private static final int MAX_MESSAGES_PER_BRAND = 15;
    
    // 브랜드별 데이터 저장소
    private final Map<String, LinkedList<JSONObject>> paymentLimitData = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<JSONObject>> sameUserData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> salesTotalData = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> topStoresData = new ConcurrentHashMap<>();
    
    public BrandDataManager() {
        // 모든 브랜드에 대해 초기 빈 리스트 생성
        for (String brand : BRANDS) {
            paymentLimitData.put(brand, new LinkedList<>());
            sameUserData.put(brand, new LinkedList<>());
        }
        System.out.println("BrandDataManager 초기화 완료. 관리 브랜드 수: " + BRANDS.size());
    }
    
    // 결제 한도 데이터 추가
    public void addPaymentLimitData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (BRANDS.contains(brand)) {
            LinkedList<JSONObject> brandData = paymentLimitData.get(brand);
            synchronized (brandData) {
                brandData.addFirst(data);
                // 15개 초과 시 오래된 데이터 제거
                while (brandData.size() > MAX_MESSAGES_PER_BRAND) {
                    brandData.removeLast();
                }
            }
            System.out.println("결제 한도 데이터 추가 - 브랜드: " + brand + ", 현재 개수: " + brandData.size());
        } else {
            System.out.println("알 수 없는 브랜드의 결제 한도 데이터: '" + brand + "'");
        }
    }
    
    // 동일인 결제 데이터 추가
    public void addSameUserData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (BRANDS.contains(brand)) {
            LinkedList<JSONObject> brandData = sameUserData.get(brand);
            synchronized (brandData) {
                brandData.addFirst(data);
                // 15개 초과 시 오래된 데이터 제거
                while (brandData.size() > MAX_MESSAGES_PER_BRAND) {
                    brandData.removeLast();
                }
            }
            System.out.println("동일인 결제 데이터 추가 - 브랜드: " + brand + ", 현재 개수: " + brandData.size());
        } else {
            System.out.println("알 수 없는 브랜드의 동일인 결제 데이터: '" + brand + "'");
        }
    }
    
    // 매출 총합 데이터 업데이트
    public void updateSalesTotalData(JSONObject data) {
        String brand = data.optString("store_brand", "");
        if (BRANDS.contains(brand)) {
            salesTotalData.put(brand, data);
            System.out.println("매출 총합 데이터 업데이트 - 브랜드: " + brand);
        } else {
            System.out.println("알 수 없는 브랜드의 매출 총합 데이터: '" + brand + "'");
        }
    }
    
    // Top Stores 데이터 업데이트 (브랜드 추출 로직 개선)
    public void updateTopStoresData(JSONObject data) {
        String extractedBrand = extractBrandFromTopStores(data);
        
        if (extractedBrand != null && BRANDS.contains(extractedBrand)) {
            // 데이터에 브랜드 정보 추가
            data.put("store_brand", extractedBrand);
            topStoresData.put(extractedBrand, data);
            System.out.println("Top Stores 데이터 업데이트 - 브랜드: " + extractedBrand);
        } else {
            System.out.println("알 수 없는 브랜드의 Top Stores 데이터: '" + extractedBrand + "'");
        }
    }
    
    // Top Stores 데이터에서 브랜드 추출
    private String extractBrandFromTopStores(JSONObject data) {
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
        return new ArrayList<>(BRANDS);
    }
    
    // 브랜드가 유효한지 확인
    public boolean isValidBrand(String brand) {
        return BRANDS.contains(brand);
    }
    
    // 현재 저장된 데이터 상태 출력 (디버깅용)
    public void printCurrentStatus() {
        System.out.println("=== BrandDataManager 현재 상태 ===");
        for (String brand : BRANDS) {
            int paymentLimitCount = paymentLimitData.get(brand).size();
            int sameUserCount = sameUserData.get(brand).size();
            boolean hasSalesTotal = salesTotalData.containsKey(brand);
            boolean hasTopStores = topStoresData.containsKey(brand);
            
            System.out.println(String.format("브랜드: %s | 결제한도: %d개 | 동일인: %d개 | 매출총합: %s | Top매장: %s", 
                brand, paymentLimitCount, sameUserCount, 
                hasSalesTotal ? "있음" : "없음", hasTopStores ? "있음" : "없음"));
        }
        System.out.println("================================");
    }
    
    // 특정 브랜드의 모든 데이터 삭제 (필요시 사용)
    public void clearBrandData(String brand) {
        if (BRANDS.contains(brand)) {
            paymentLimitData.get(brand).clear();
            sameUserData.get(brand).clear();
            salesTotalData.remove(brand);
            topStoresData.remove(brand);
            System.out.println("브랜드 데이터 삭제 완료: " + brand);
        }
    }
    
    // 모든 브랜드 데이터 삭제 (필요시 사용)
    public void clearAllData() {
        for (String brand : BRANDS) {
            clearBrandData(brand);
        }
        System.out.println("모든 브랜드 데이터 삭제 완료");
    }
}
