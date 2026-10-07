package com.pokerplanning.ai.api.dto;

import com.pokerplanning.ai.domain.ComplexityLevel;
import com.pokerplanning.ai.domain.RiskLevel;
import com.pokerplanning.ai.domain.StoryAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Résultat complet de l'analyse d'une User Story par l'assistant IA")
public record StoryAnalysisResponse(
    @Schema(description = "Identifiant de la story analysée", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    UUID storyId,

    @Schema(description = "Clé lisible de la story", example = "US-1")
    String storyKey,

    @Schema(description = "Niveau de complexité évalué", example = "MEDIUM")
    ComplexityLevel complexity,

    @Schema(description = "Niveau de risque technique", example = "LOW")
    RiskLevel riskLevel,

    @Schema(description = "Score de clarté global (0 à 100)", example = "85")
    int clarityScore,

    @Schema(description = "Fourchette d'estimation suggérée", example = "3 – 5")
    String suggestedEstimateRange,

    @Schema(description = "Explication justifiant la fourchette d'estimation", example = "Périmètre standard de développement avec 3 critère(s) d'acceptation identifié(s).")
    String estimateRationale,

    @Schema(description = "Alertes sur les critères d'acceptation manquants ou incomplets")
    List<String> missingAcceptanceCriteriaAlerts,

    @Schema(description = "Questions de clarification suggérées")
    List<ClarificationQuestionDto> clarificationQuestions,

    @Schema(description = "Facteurs de risque technique identifiés")
    List<String> detectedRiskFactors
) {
    public static StoryAnalysisResponse fromDomain(StoryAnalysis analysis) {
        return new StoryAnalysisResponse(
            analysis.storyId(),
            analysis.storyKey(),
            analysis.complexity(),
            analysis.riskLevel(),
            analysis.clarityScore(),
            analysis.suggestedEstimateRange(),
            analysis.estimateRationale(),
            analysis.missingAcceptanceCriteriaAlerts(),
            analysis.clarificationQuestions().stream()
                .map(q -> new ClarificationQuestionDto(q.category(), q.question()))
                .toList(),
            analysis.detectedRiskFactors()
        );
    }
}
