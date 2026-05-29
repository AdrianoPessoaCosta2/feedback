package com.feedback.api.model;

import java.time.Instant;

public record FeedbackMessage(
        String descricao,
        int nota,
        String urgencia,
        Instant dataEnvio
) {
}
