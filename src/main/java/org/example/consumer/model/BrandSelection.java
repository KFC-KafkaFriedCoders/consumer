package org.example.consumer.model;

public class BrandSelection {
    private String brand;
    
    // 기본 생성자
    public BrandSelection() {
    }
    
    public BrandSelection(String brand) {
        this.brand = brand;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
}
