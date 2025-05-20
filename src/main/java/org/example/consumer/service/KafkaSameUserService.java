package org.example.consumer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaSameUserService extends AbstractKafkaConsumerService {

    @Autowired
    public KafkaSameUserService(BatchProcessorService batchProcessor) {
        super(batchProcessor);
    }

    @Override
    protected String getTopic() {
        return "same_user_payment";
    }

    @Override
    protected String getGroupId() {
        return "same-user-consumer-group11";
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
        return batchProcessor.queueSameUserMessage(jsonObject);
    }
}
