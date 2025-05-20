package org.example.consumer.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaSalesTotalService extends AbstractKafkaConsumerService {

    @Autowired
    public KafkaSalesTotalService(BatchProcessorService batchProcessor) {
        super(batchProcessor);
    }

    @Override
    protected String getTopic() {
        return "sales_total";
    }

    @Override
    protected String getGroupId() {
        return "sales-total-consumer-group11";
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
        return 200;
    }

    @Override
    protected int getPollTimeoutMs() {
        return 200;
    }

    @Override
    protected CompletableFuture<Void> processMessage(JSONObject jsonObject) {
        return batchProcessor.queueSalesTotalMessage(jsonObject);
    }
}
