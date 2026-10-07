package com.pokerplanning.estimation.api.dto;

import jakarta.validation.constraints.NotBlank;

public record FinalizeEstimateRequest(
    @NotBlank(message = "L'estimation finale ne peut pas être vide")
    String finalEstimate
) {
}
