package org.example.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "brand")
public class BrandConfig {
    private List<String> names;
    private int maxMessagesPerBrand = 15;
    
    public List<String> getNames() {
        return names;
    }
    
    public void setNames(List<String> names) {
        this.names = names;
    }
    
    public int getMaxMessagesPerBrand() {
        return maxMessagesPerBrand;
    }
    
    public void setMaxMessagesPerBrand(int maxMessagesPerBrand) {
        this.maxMessagesPerBrand = maxMessagesPerBrand;
    }
}
