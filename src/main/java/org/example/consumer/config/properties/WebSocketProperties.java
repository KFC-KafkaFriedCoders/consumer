package org.example.consumer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {
    private String allowedOrigins = "*";
    private String endpoint = "/payment-limit-ws";
    private String brokerPrefixes;
    private String appDestinationPrefix = "/app";
    private String userDestinationPrefix = "/user";
    
    private List<String> brokerPrefixList;
    
    public String getAllowedOrigins() {
        return allowedOrigins;
    }
    
    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getBrokerPrefixes() {
        return brokerPrefixes;
    }
    
    public void setBrokerPrefixes(String brokerPrefixes) {
        this.brokerPrefixes = brokerPrefixes;
        this.brokerPrefixList = Arrays.asList(brokerPrefixes.split(","));
    }
    
    public List<String> getBrokerPrefixList() {
        if (brokerPrefixList == null) {
            brokerPrefixList = new ArrayList<>();
        }
        return brokerPrefixList;
    }
    
    public String getAppDestinationPrefix() {
        return appDestinationPrefix;
    }
    
    public void setAppDestinationPrefix(String appDestinationPrefix) {
        this.appDestinationPrefix = appDestinationPrefix;
    }
    
    public String getUserDestinationPrefix() {
        return userDestinationPrefix;
    }
    
    public void setUserDestinationPrefix(String userDestinationPrefix) {
        this.userDestinationPrefix = userDestinationPrefix;
    }
}
