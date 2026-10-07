package com.pokerplanning.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "Demande d'analyse ad-hoc de texte sans persistance préalable")
public record AnalyzeTextRequest(
    @NotBlank(message = "Le titre ne doit pas être vide")
    @Schema(description = "Titre de la story", example = "Intégration du paiement Stripe")
    String title,

    @Schema(description = "Description détaillée de la story", example = "En tant que client, je veux régler par carte bancaire...")
    String description,

    @Schema(description = "Liste des critères d'acceptation", example = "[\"Gérer le webhook Stripe\", \"Notifier l'utilisateur en cas d'échec\"]")
    List<String> acceptanceCriteria
) {}
