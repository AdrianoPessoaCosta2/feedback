package com.feedback.api.controller;

import com.feedback.api.model.Avaliacao;
import com.feedback.api.model.AvaliacaoResponse;
import com.feedback.api.model.FeedbackMessage;
import com.feedback.api.service.SnsPublisher;
import com.feedback.api.service.UrgenciaService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

@Path("/avaliacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvaliacaoController {

    @Inject
    UrgenciaService urgenciaService;

    @Inject
    SnsPublisher snsPublisher;

    @POST
    public Response criar(@Valid Avaliacao avaliacao) {
        Instant dataEnvio = Instant.now();
        String urgencia = urgenciaService.classificar(avaliacao.nota());

        FeedbackMessage message = new FeedbackMessage(
                avaliacao.descricao(),
                avaliacao.nota(),
                urgencia,
                dataEnvio
        );

        String messageId = snsPublisher.publish(message);

        AvaliacaoResponse response = new AvaliacaoResponse(
                avaliacao.descricao(),
                avaliacao.nota(),
                urgencia,
                dataEnvio,
                messageId
        );

        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
