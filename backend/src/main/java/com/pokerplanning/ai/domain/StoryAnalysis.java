package com.pokerplanning.ai.domain;

import java.util.List;
import java.util.UUID;

public record StoryAnalysis(
    UUID storyId,
    String storyKey,
    ComplexityLevel complexity,
    RiskLevel riskLevel,
    int clarityScore,
    String suggestedEstimateRange,
    String estimateRationale,
    List<String> missingAcceptanceCriteriaAlerts,
    List<ClarificationQuestion> clarificationQuestions,
    List<String> detectedRiskFactors
) {
    public StoryAnalysis {
        missingAcceptanceCriteriaAlerts = missingAcceptanceCriteriaAlerts != null ? List.copyOf(missingAcceptanceCriteriaAlerts) : List.of();
        clarificationQuestions = clarificationQuestions != null ? List.copyOf(clarificationQuestions) : List.of();
        detectedRiskFactors = detectedRiskFactors != null ? List.copyOf(detectedRiskFactors) : List.of();
    }
}
