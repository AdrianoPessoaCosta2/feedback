package com.feedback.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

public class EmailHandler implements RequestHandler<SNSEvent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final SesClient SES = SesClient.builder()
            .region(Region.of(System.getenv("AWS_REGION")))
            .build();

    private static final String SENDER = System.getenv("SENDER_EMAIL");
    private static final String RECIPIENT = System.getenv("ADMIN_EMAIL");

    @Override
    public String handleRequest(SNSEvent event, Context context) {
        for (SNSEvent.SNSRecord record : event.getRecords()) {
            String message = record.getSNS().getMessage();
            context.getLogger().log("Received SNS message: " + message);

            try {
                FeedbackMessage feedback = MAPPER.readValue(message, FeedbackMessage.class);

                if ("CRITICA".equals(feedback.getUrgencia()) || "ALTA".equals(feedback.getUrgencia())) {
                    sendUrgentEmail(feedback, context);
                } else {
                    context.getLogger().log("Skipping non-urgent feedback: urgencia=" + feedback.getUrgencia());
                }
            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return "OK";
    }

    private void sendUrgentEmail(FeedbackMessage feedback, Context context) {
        String subject = String.format("[URGENTE] Feedback %s recebido", feedback.getUrgencia());
        String body = String.format(
                """
                Feedback urgente recebido!

                Descrição: %s
                Nota: %d
                Urgência: %s
                Data de envio: %s

                Ação imediata necessária.
                """,
                feedback.getDescricao(),
                feedback.getNota(),
                feedback.getUrgencia(),
                feedback.getDataEnvio()
        );

        SendEmailRequest request = SendEmailRequest.builder()
                .source(SENDER)
                .destination(Destination.builder().toAddresses(RECIPIENT).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .text(Content.builder().data(body).charset("UTF-8").build())
                                .build())
                        .build())
                .build();

        SES.sendEmail(request);
        context.getLogger().log("Urgent email sent for feedback: urgencia=" + feedback.getUrgencia());
    }
}
