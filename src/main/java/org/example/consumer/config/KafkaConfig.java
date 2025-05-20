package org.example.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    private String bootstrapServers = "13.209.157.53:9092,15.164.111.153:9092,3.34.32.69:9092";
    
    public String getBootstrapServers() {
        return bootstrapServers;
    }
    
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }
}
