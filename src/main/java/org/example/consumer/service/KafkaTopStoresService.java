package org.example.consumer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaTopStoresService extends AbstractKafkaConsumerService {

    @Autowired
    public KafkaTopStoresService(BatchProcessorService batchProcessor) {
        super(batchProcessor);
    }

    @Override
    protected String getTopic() {
        return "top_stores";
    }

    @Override
    protected String getGroupId() {
        return "top-stores-consumer-group11";
    }

    @Override
    protected int getBatchSize() {
        return 5;
    }

    @Override
    protected int getFetchMinBytes() {
        return 512;
    }

    @Override
    protected int getFetchMaxWaitMs() {
        return 300;
    }

    @Override
    protected int getPollTimeoutMs() {
        return 300;
    }

    @Override
    protected CompletableFuture<Void> processMessage(JSONObject jsonObject) {
        return batchProcessor.queueTopStoresMessage(jsonObject);
    }
}
