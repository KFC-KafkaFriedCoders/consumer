package org.example.consumer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BatchProcessorService {

    private static final int BATCH_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WebSocketService webSocketService;
    private final BrandDataManager brandDataManager;

    private final BlockingQueue<MessageBatch> paymentLimitBatch = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final BlockingQueue<MessageBatch> sameUserBatch = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final BlockingQueue<MessageBatch> salesTotalBatch = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final BlockingQueue<MessageBatch> topStoresBatch = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    private final AtomicBoolean processingActive = new AtomicBoolean(true);

    @Autowired
    public BatchProcessorService(WebSocketService webSocketService, BrandDataManager brandDataManager) {
        this.webSocketService = webSocketService;
        this.brandDataManager = brandDataManager;
    }

    @Async("batchExecutor")
    public CompletableFuture<Void> queuePaymentLimitMessage(JSONObject message) {
        return CompletableFuture.runAsync(() -> {
            try {
                message.put("server_received_time", LocalDateTime.now().format(formatter));
                message.put("event_type", "payment_limit_alert");
                if (!message.has("id")) {
                    message.put("id", System.currentTimeMillis() + Math.random());
                }
                
                MessageBatch batch = new MessageBatch(message, MessageType.PAYMENT_LIMIT);
                paymentLimitBatch.offer(batch);
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    @Async("batchExecutor")
    public CompletableFuture<Void> queueSameUserMessage(JSONObject message) {
        return CompletableFuture.runAsync(() -> {
            try {
                message.put("server_received_time", LocalDateTime.now().format(formatter));
                message.put("event_type", "payment_same_user_alert");
                if (!message.has("id")) {
                    message.put("id", System.currentTimeMillis() + Math.random());
                }
                
                MessageBatch batch = new MessageBatch(message, MessageType.SAME_USER);
                sameUserBatch.offer(batch);
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    @Async("batchExecutor")
    public CompletableFuture<Void> queueSalesTotalMessage(JSONObject message) {
        return CompletableFuture.runAsync(() -> {
            try {
                message.put("server_received_time", LocalDateTime.now().format(formatter));
                message.put("event_type", "sales_total_update");
                if (!message.has("id")) {
                    message.put("id", System.currentTimeMillis() + Math.random());
                }
                
                MessageBatch batch = new MessageBatch(message, MessageType.SALES_TOTAL);
                salesTotalBatch.offer(batch);
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    @Async("batchExecutor")
    public CompletableFuture<Void> queueTopStoresMessage(JSONObject message) {
        return CompletableFuture.runAsync(() -> {
            try {
                message.put("server_received_time", LocalDateTime.now().format(formatter));
                message.put("event_type", "top_stores_update");
                if (!message.has("id")) {
                    message.put("id", System.currentTimeMillis() + "-" + Math.random());
                }
                
                MessageBatch batch = new MessageBatch(message, MessageType.TOP_STORES);
                topStoresBatch.offer(batch);
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    @Scheduled(fixedDelay = 100)
    public void processBatches() {
        if (!processingActive.get()) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        futures.add(processBatchQueue(paymentLimitBatch, MessageType.PAYMENT_LIMIT));
        futures.add(processBatchQueue(sameUserBatch, MessageType.SAME_USER));
        futures.add(processBatchQueue(salesTotalBatch, MessageType.SALES_TOTAL));
        futures.add(processBatchQueue(topStoresBatch, MessageType.TOP_STORES));

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Async("batchExecutor")
    private CompletableFuture<Void> processBatchQueue(BlockingQueue<MessageBatch> queue, MessageType type) {
        return CompletableFuture.runAsync(() -> {
            List<MessageBatch> messages = new ArrayList<>();
            queue.drainTo(messages, BATCH_SIZE);

            if (!messages.isEmpty()) {
                switch (type) {
                    case PAYMENT_LIMIT:
                        processPaymentLimitBatch(messages);
                        break;
                    case SAME_USER:
                        processSameUserBatch(messages);
                        break;
                    case SALES_TOTAL:
                        processSalesTotalBatch(messages);
                        break;
                    case TOP_STORES:
                        processTopStoresBatch(messages);
                        break;
                }
            }
        });
    }

    private void processPaymentLimitBatch(List<MessageBatch> messages) {
        Map<String, List<JSONObject>> brandGroups = groupMessagesByBrand(messages);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Map.Entry<String, List<JSONObject>> entry : brandGroups.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                for (JSONObject message : entry.getValue()) {
                    brandDataManager.addPaymentLimitData(message);
                }
            }).thenCompose(v -> 
                webSocketService.sendPaymentLimitBatchAsync(entry.getValue())
            ));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processSameUserBatch(List<MessageBatch> messages) {
        Map<String, List<JSONObject>> brandGroups = groupMessagesByBrand(messages);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Map.Entry<String, List<JSONObject>> entry : brandGroups.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                for (JSONObject message : entry.getValue()) {
                    brandDataManager.addSameUserData(message);
                }
            }).thenCompose(v -> 
                webSocketService.sendSameUserBatchAsync(entry.getValue())
            ));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processSalesTotalBatch(List<MessageBatch> messages) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (MessageBatch batch : messages) {
            futures.add(CompletableFuture.runAsync(() -> {
                brandDataManager.updateSalesTotalData(batch.getMessage());
            }).thenCompose(v -> 
                webSocketService.sendSalesTotalAsync(batch.getMessage())
            ));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processTopStoresBatch(List<MessageBatch> messages) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (MessageBatch batch : messages) {
            futures.add(CompletableFuture.runAsync(() -> {
                brandDataManager.updateTopStoresData(batch.getMessage());
            }).thenCompose(v -> 
                webSocketService.sendTopStoresAsync(batch.getMessage())
            ));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private Map<String, List<JSONObject>> groupMessagesByBrand(List<MessageBatch> messages) {
        Map<String, List<JSONObject>> brandGroups = new ConcurrentHashMap<>();
        
        for (MessageBatch batch : messages) {
            String brand = batch.getMessage().optString("store_brand", "Unknown");
            brandGroups.computeIfAbsent(brand, k -> new ArrayList<>()).add(batch.getMessage());
        }
        
        return brandGroups;
    }

    public void shutdown() {
        processingActive.set(false);
    }

    private static class MessageBatch {
        private final JSONObject message;
        private final MessageType type;

        public MessageBatch(JSONObject message, MessageType type) {
            this.message = message;
            this.type = type;
        }

        public JSONObject getMessage() {
            return message;
        }

        public MessageType getType() {
            return type;
        }
    }

    private enum MessageType {
        PAYMENT_LIMIT, SAME_USER, SALES_TOTAL, TOP_STORES
    }
}