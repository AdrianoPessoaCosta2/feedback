package com.feedback.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Avaliacao(
        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @NotNull(message = "nota é obrigatória")
        @Min(value = 0, message = "nota mínima é 0")
        @Max(value = 10, message = "nota máxima é 10")
        Integer nota
) {
}
