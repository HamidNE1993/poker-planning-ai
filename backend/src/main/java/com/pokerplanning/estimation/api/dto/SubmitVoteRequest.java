package com.pokerplanning.estimation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitVoteRequest(
    @NotNull(message = "L'identifiant du participant est requis")
    UUID participantId,

    @NotBlank(message = "La valeur du vote ne peut pas être vide")
    String voteValue
) {
}
