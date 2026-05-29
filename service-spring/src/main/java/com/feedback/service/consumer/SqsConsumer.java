package com.feedback.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedback.service.model.FeedbackItem;
import com.feedback.service.model.FeedbackMessage;
import com.feedback.service.repository.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SqsConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsConsumer.class);

    private final SqsClient sqsClient;
    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public SqsConsumer(SqsClient sqsClient, FeedbackRepository feedbackRepository, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.feedbackRepository = feedbackRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${sqs.poll.interval:5000}")
    public void pollMessages() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5)
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {
            try {
                String body = message.body();
                Map<String, Object> snsWrapper = objectMapper.readValue(body, Map.class);
                String feedbackJson = (String) snsWrapper.get("Message");

                FeedbackMessage feedback = objectMapper.readValue(feedbackJson, FeedbackMessage.class);

                FeedbackItem item = new FeedbackItem();
                item.setId(UUID.randomUUID().toString());
                item.setDescricao(feedback.descricao());
                item.setNota(feedback.nota());
                item.setUrgencia(feedback.urgencia());
                item.setDataEnvio(feedback.dataEnvio().toString());

                feedbackRepository.save(item);
                log.info("Saved feedback: id={}, urgencia={}", item.getId(), item.getUrgencia());

                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            } catch (Exception e) {
                log.error("Error processing SQS message: {}", e.getMessage(), e);
            }
        }
    }
}
