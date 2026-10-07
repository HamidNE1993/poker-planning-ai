package com.pokerplanning.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Question de clarification suggérée pour guider les débats de l'équipe")
public record ClarificationQuestionDto(
    @Schema(description = "Catégorie de la question (Sécurité, Données, Ergonomie, etc.)", example = "Sécurité")
    String category,

    @Schema(description = "Libellé de la question", example = "Quelles sont les permissions requises pour exécuter cette action ?")
    String question
) {}
