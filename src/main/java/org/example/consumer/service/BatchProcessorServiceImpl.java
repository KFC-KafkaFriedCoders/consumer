package org.example.consumer.service;

import org.example.consumer.service.interfaces.BatchProcessorService;
import org.example.consumer.service.interfaces.BrandDataService;
import org.example.consumer.service.interfaces.MessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class BatchProcessorServiceImpl implements BatchProcessorService {
    private static final Logger log = LoggerFactory.getLogger(BatchProcessorServiceImpl.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final BrandDataService brandDataService;
    
    private final Map<MessageType, BlockingQueue<JSONObject>> messageQueues = new ConcurrentHashMap<>();
    
    @Autowired
    public BatchProcessorServiceImpl(SimpMessagingTemplate messagingTemplate,
                                     BrandDataService brandDataService) {
        this.messagingTemplate = messagingTemplate;
        this.brandDataService = brandDataService;
        
        // 각 메시지 타입별 큐 초기화
        for (MessageType type : MessageType.values()) {
            messageQueues.put(type, new LinkedBlockingQueue<>());
        }
    }
    
    @PostConstruct
    public void init() {
        log.info("BatchProcessorService 초기화 완료");
    }
    
    @Override
    @Async("taskExecutor")
    public void addToQueue(MessageType type, JSONObject message) {
        try {
            messageQueues.get(type).offer(message);
        } catch (Exception e) {
            log.error("메시지 큐 추가 오류: {}", e.getMessage());
        }
    }
    
    @Override
    @Scheduled(fixedDelayString = "${app.batch.flush-interval-ms:2000}")
    public void processQueues() {
        for (MessageType type : MessageType.values()) {
            processBatch(type);
        }
    }
    
    private void processBatch(MessageType type) {
        BlockingQueue<JSONObject> queue = messageQueues.get(type);
        if (queue.isEmpty()) {
            return;
        }
        
        List<JSONObject> batch = new ArrayList<>();
        queue.drainTo(batch);
        
        if (batch.isEmpty()) {
            return;
        }
        
        log.debug("{} 타입 배치 처리: {} 개의 메시지", type, batch.size());
        
        try {
            switch (type) {
                case PAYMENT_LIMIT:
                    processBatchPaymentLimit(batch);
                    break;
                case SAME_USER:
                    processBatchSameUser(batch);
                    break;
                case SALES_TOTAL:
                    processBatchSalesTotal(batch);
                    break;
                case TOP_STORES:
                    processBatchTopStores(batch);
                    break;
            }
        } catch (Exception e) {
            log.error("배치 처리 중 오류 발생: {}", e.getMessage());
        }
    }
    
    private void processBatchPaymentLimit(List<JSONObject> batch) {
        for (JSONObject message : batch) {
            brandDataService.addPaymentLimitData(message);
            messagingTemplate.convertAndSend("/topic/payment-limit", message.toString());
        }
    }
    
    private void processBatchSameUser(List<JSONObject> batch) {
        for (JSONObject message : batch) {
            brandDataService.addSameUserData(message);
            messagingTemplate.convertAndSend("/topic/payment-same-user", message.toString());
        }
    }
    
    private void processBatchSalesTotal(List<JSONObject> batch) {
        for (JSONObject message : batch) {
            brandDataService.updateSalesTotalData(message);
            messagingTemplate.convertAndSend("/topic/sales-total", message.toString());
        }
    }
    
    private void processBatchTopStores(List<JSONObject> batch) {
        for (JSONObject message : batch) {
            brandDataService.updateTopStoresData(message);
            messagingTemplate.convertAndSend("/topic/top-stores", message.toString());
        }
    }
    
    public static class MessageBatch {
        private final MessageType type;
        private final List<JSONObject> messages;
        
        public MessageBatch(MessageType type, List<JSONObject> messages) {
            this.type = type;
            this.messages = messages;
        }
        
        public MessageType getType() {
            return type;
        }
        
        public List<JSONObject> getMessages() {
            return messages;
        }
    }
}
