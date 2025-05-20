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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Service
public class BatchProcessorService {

    private static final Logger logger = Logger.getLogger(BatchProcessorService.class.getName());
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
                JSONObject processedMessage = prepareMessage(message, "payment_limit_alert");
                MessageBatch batch = new MessageBatch(processedMessage, MessageType.PAYMENT_LIMIT);
                
                if (!paymentLimitBatch.offer(batch)) {
                    logger.warning("Payment limit queue is full, dropping message");
                }
            } catch (Exception e) {
                logger.severe("Error queuing payment limit message: " + e.getMessage());
            }
        });
    }

    @Async("batchExecutor")
    public CompletableFuture<Void> queueSameUserMessage(JSONObject message) {
        return CompletableFuture.runAsync(() -> {
            try {
                JSONObject processedMessage = prepareMessage(message, "payment_same_user_alert");
                MessageBatch batch = new MessageBatch(processedMessage, MessageType.SAME_USER);
                
                if (!sameUserBatch.offer(batch)) {
                    logger.warning("Same user queue is full, dropping message");
                }
            } catch (Exception e) {
                logger.severe("Error queuing same user message: " + e.getMessage());
            }
        });
    }

    @Async("batchExecutor")
    public CompletableFuture<Void> queueSalesTotalMessage(JSONObject message) {
        return CompletableFuture.runAsync(() -> {
            try {
                JSONObject processedMessage = prepareMessage(message, "sales_total_update");
                MessageBatch batch = new MessageBatch(processedMessage, MessageType.SALES_TOTAL);
                
                if (!salesTotalBatch.offer(batch)) {
                    logger.warning("Sales total queue is full, dropping message");
                }
            } catch (Exception e) {
                logger.severe("Error queuing sales total message: " + e.getMessage());
            }
        });
    }

    @Async("batchExecutor")
    public CompletableFuture<Void> queueTopStoresMessage(JSONObject message) {
        return CompletableFuture.runAsync(() -> {
            try {
                JSONObject processedMessage = prepareMessage(message, "top_stores_update");
                MessageBatch batch = new MessageBatch(processedMessage, MessageType.TOP_STORES);
                
                if (!topStoresBatch.offer(batch)) {
                    logger.warning("Top stores queue is full, dropping message");
                }
            } catch (Exception e) {
                logger.severe("Error queuing top stores message: " + e.getMessage());
            }
        });
    }

    @Scheduled(fixedDelay = 100)
    public void processBatches() {
        if (!processingActive.get()) return;

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            futures.add(processBatchQueue(paymentLimitBatch, MessageType.PAYMENT_LIMIT));
            futures.add(processBatchQueue(sameUserBatch, MessageType.SAME_USER));
            futures.add(processBatchQueue(salesTotalBatch, MessageType.SALES_TOTAL));
            futures.add(processBatchQueue(topStoresBatch, MessageType.TOP_STORES));

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            logger.severe("Error processing batches: " + e.getMessage());
        }
    }

    public void shutdown() {
        processingActive.set(false);
    }

    @Async("batchExecutor")
    private CompletableFuture<Void> processBatchQueue(BlockingQueue<MessageBatch> queue, MessageType type) {
        return CompletableFuture.runAsync(() -> {
            try {
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
            } catch (Exception e) {
                logger.severe("Error processing batch queue for type " + type + ": " + e.getMessage());
            }
        });
    }

    private void processPaymentLimitBatch(List<MessageBatch> messages) {
        try {
            // 전체 브로드캐스트 제거, 개별 처리만 수행
            for (MessageBatch batch : messages) {
                webSocketService.sendPaymentLimitAlert(batch.getMessage());
            }
        } catch (Exception e) {
            logger.severe("Error processing payment limit batch: " + e.getMessage());
        }
    }

    private void processSameUserBatch(List<MessageBatch> messages) {
        try {
            // 전체 브로드캐스트 제거, 개별 처리만 수행
            for (MessageBatch batch : messages) {
                webSocketService.sendSameUserAlert(batch.getMessage());
            }
        } catch (Exception e) {
            logger.severe("Error processing same user batch: " + e.getMessage());
        }
    }

    private void processSalesTotalBatch(List<MessageBatch> messages) {
        try {
            // 개별 처리만 수행 (브랜드별 필터링은 WebSocketService에서 처리)
            for (MessageBatch batch : messages) {
                webSocketService.sendSalesTotalData(batch.getMessage());
            }
        } catch (Exception e) {
            logger.severe("Error processing sales total batch: " + e.getMessage());
        }
    }

    private void processTopStoresBatch(List<MessageBatch> messages) {
        try {
            // 개별 처리만 수행 (브랜드별 필터링은 WebSocketService에서 처리)
            for (MessageBatch batch : messages) {
                webSocketService.sendTopStoresData(batch.getMessage());
            }
        } catch (Exception e) {
            logger.severe("Error processing top stores batch: " + e.getMessage());
        }
    }

    private JSONObject prepareMessage(JSONObject message, String eventType) {
        try {
            message.put("server_received_time", LocalDateTime.now().format(formatter));
            message.put("event_type", eventType);
            if (!message.has("id")) {
                message.put("id", System.currentTimeMillis() + Math.random());
            }
            return message;
        } catch (Exception e) {
            logger.warning("Error preparing message: " + e.getMessage());
            return message;
        }
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
