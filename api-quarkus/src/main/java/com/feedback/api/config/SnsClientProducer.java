package com.feedback.api.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class SnsClientProducer {

    @ConfigProperty(name = "aws.region")
    String region;

    @ConfigProperty(name = "aws.sns.endpoint-override")
    Optional<String> endpointOverride;

    @Produces
    @ApplicationScoped
    public SnsClient snsClient() {
        var builder = SnsClient.builder()
                .region(Region.of(region));
        endpointOverride.ifPresent(uri -> builder.endpointOverride(URI.create(uri)));
        return builder.build();
    }
}
