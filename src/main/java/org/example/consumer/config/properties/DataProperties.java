package org.example.consumer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.data")
public class DataProperties {
    private int maxMessagesPerBrand = 15;
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    
    public int getMaxMessagesPerBrand() {
        return maxMessagesPerBrand;
    }
    
    public void setMaxMessagesPerBrand(int maxMessagesPerBrand) {
        this.maxMessagesPerBrand = maxMessagesPerBrand;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
