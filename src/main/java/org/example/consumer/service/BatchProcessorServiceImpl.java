package org.example.consumer.service;

import org.example.consumer.service.interfaces.BatchProcessorService;
import org.example.consumer.service.interfaces.BrandDataService;
import org.example.consumer.service.interfaces.MessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.json.JSONArray;
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
            if (message == null) {
                log.warn("메시지 큐에 null 메시지가 추가되었습니다. 타입: {}", type);
                return;
            }
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
            if (message == null) {
                log.warn("결제 한도 배치에 null 메시지가 포함되어 있습니다.");
                continue;
            }
            
            try {
                brandDataService.addPaymentLimitData(message);
                messagingTemplate.convertAndSend("/topic/payment-limit", message.toString());
            } catch (Exception e) {
                log.error("결제 한도 데이터 처리 중 오류: {}", e.getMessage());
            }
        }
    }
    
    private void processBatchSameUser(List<JSONObject> batch) {
        for (JSONObject message : batch) {
            if (message == null) {
                log.warn("동일 사용자 배치에 null 메시지가 포함되어 있습니다.");
                continue;
            }
            
            try {
                brandDataService.addSameUserData(message);
                messagingTemplate.convertAndSend("/topic/payment-same-user", message.toString());
            } catch (Exception e) {
                log.error("동일 사용자 데이터 처리 중 오류: {}", e.getMessage());
            }
        }
    }
    
    private void processBatchSalesTotal(List<JSONObject> batch) {
        for (JSONObject message : batch) {
            if (message == null) {
                log.warn("매출 총합 배치에 null 메시지가 포함되어 있습니다.");
                continue;
            }
            
            try {
                brandDataService.updateSalesTotalData(message);
                messagingTemplate.convertAndSend("/topic/sales-total", message.toString());
            } catch (Exception e) {
                log.error("매출 총합 데이터 처리 중 오류: {}", e.getMessage());
            }
        }
    }
    
    private void processBatchTopStores(List<JSONObject> batch) {
        for (JSONObject message : batch) {
            if (message == null) {
                log.warn("Top Stores 배치에 null 메시지가 포함되어 있습니다.");
                continue;
            }
            
            try {
                // 메시지가 유효한지 추가 검사
                if (!message.has("top_stores")) {
                    log.warn("Top Stores 메시지에 top_stores 배열이 없습니다: {}", message);
                    continue;
                }
                
                // top_stores 배열이 비어있는지 확인
                JSONArray topStores = message.getJSONArray("top_stores");
                if (topStores.length() == 0) {
                    log.warn("Top Stores 배열이 비어있습니다: {}", message);
                    continue;
                }
                
                // 첫 번째 매장에 store_brand가 있는지 확인
                JSONObject firstStore = topStores.getJSONObject(0);
                if (!firstStore.has("store_brand")) {
                    log.warn("Top Stores 매장에 store_brand 필드가 없습니다: {}", firstStore);
                    continue;
                }
                
                // 유효성 검증이 완료된 메시지를 처리
                brandDataService.updateTopStoresData(message);
                messagingTemplate.convertAndSend("/topic/top-stores", message.toString());
            } catch (Exception e) {
                log.error("Top Stores 데이터 처리 중 오류: {}", e.getMessage());
            }
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
