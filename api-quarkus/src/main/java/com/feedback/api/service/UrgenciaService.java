package com.feedback.api.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UrgenciaService {

    public String classificar(int nota) {
        if (nota <= 3) {
            return "CRITICA";
        } else if (nota <= 5) {
            return "ALTA";
        } else if (nota <= 7) {
            return "MEDIA";
        }
        return "BAIXA";
    }
}
