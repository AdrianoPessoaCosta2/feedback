package com.feedback.api.model;

import java.time.Instant;

public record AvaliacaoResponse(
        String descricao,
        int nota,
        String urgencia,
        Instant dataEnvio,
        String messageId
) {
}
