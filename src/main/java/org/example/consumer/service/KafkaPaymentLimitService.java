package org.example.consumer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaPaymentLimitService extends AbstractKafkaConsumerService {

    @Autowired
    public KafkaPaymentLimitService(BatchProcessorService batchProcessor) {
        super(batchProcessor);
    }

    @Override
    protected String getTopic() {
        return "payment_limit";
    }

    @Override
    protected String getGroupId() {
        return "payment-limit-consumer-group11";
    }

    @Override
    protected int getBatchSize() {
        return 10;
    }

    @Override
    protected int getFetchMinBytes() {
        return 1024;
    }

    @Override
    protected int getFetchMaxWaitMs() {
        return 100;
    }

    @Override
    protected int getPollTimeoutMs() {
        return 100;
    }

    @Override
    protected CompletableFuture<Void> processMessage(JSONObject jsonObject) {
        return batchProcessor.queuePaymentLimitMessage(jsonObject);
    }
}
