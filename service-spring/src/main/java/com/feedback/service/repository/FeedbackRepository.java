package com.feedback.service.repository;

import com.feedback.service.model.FeedbackItem;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class FeedbackRepository {

    private final DynamoDbEnhancedClient enhancedClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    private DynamoDbTable<FeedbackItem> table;

    public FeedbackRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    @PostConstruct
    void init() {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(FeedbackItem.class));
    }

    public void save(FeedbackItem item) {
        table.putItem(item);
    }

    public List<FeedbackItem> findAll() {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    public List<FeedbackItem> findByDateRange(String startDate, String endDate) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(item -> item.getDataEnvio().compareTo(startDate) >= 0
                        && item.getDataEnvio().compareTo(endDate) <= 0)
                .collect(Collectors.toList());
    }
}
