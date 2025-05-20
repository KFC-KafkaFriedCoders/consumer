package org.example.consumer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.brands")
public class BrandProperties {
    private String names;
    
    private List<String> namesList;
    
    public String getNames() {
        return names;
    }
    
    public void setNames(String names) {
        this.names = names;
        this.namesList = Arrays.asList(names.split(","));
    }
    
    public List<String> getNamesList() {
        if (namesList == null) {
            namesList = new ArrayList<>();
        }
        return namesList;
    }
}
