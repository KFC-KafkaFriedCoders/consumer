package org.example.consumer.service.interfaces;

import org.json.JSONObject;

public interface BatchProcessorService {
    void addToQueue(MessageType type, JSONObject message);
    
    void processQueues();
}
