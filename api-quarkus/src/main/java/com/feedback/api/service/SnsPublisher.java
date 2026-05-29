package com.feedback.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedback.api.model.FeedbackMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

@ApplicationScoped
public class SnsPublisher {

    private static final Logger LOG = Logger.getLogger(SnsPublisher.class);

    @Inject
    SnsClient snsClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "aws.sns.topic-arn")
    String topicArn;

    public String publish(FeedbackMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(payload)
                    .messageAttributes(Map.of(
                            "urgencia", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(message.urgencia())
                                    .build()
                    ))
                    .build();

            PublishResponse response = snsClient.publish(request);
            LOG.infof("Published to SNS: messageId=%s, urgencia=%s", response.messageId(), message.urgencia());
            return response.messageId();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize feedback message", e);
        }
    }
}
