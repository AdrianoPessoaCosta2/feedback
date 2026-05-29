package com.feedback.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.sqs.endpoint-override:}")
    private String sqsEndpointOverride;

    @Value("${aws.dynamodb.endpoint-override:}")
    private String dynamoEndpointOverride;

    @Bean
    public SqsClient sqsClient() {
        var builder = SqsClient.builder().region(Region.of(region));
        if (!sqsEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(sqsEndpointOverride));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder().region(Region.of(region));
        if (!dynamoEndpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(dynamoEndpointOverride));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
