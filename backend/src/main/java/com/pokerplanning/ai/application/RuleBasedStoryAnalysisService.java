package com.pokerplanning.ai.application;

import com.pokerplanning.ai.domain.ClarificationQuestion;
import com.pokerplanning.ai.domain.ComplexityLevel;
import com.pokerplanning.ai.domain.RiskLevel;
import com.pokerplanning.ai.domain.StoryAnalysis;
import com.pokerplanning.story.domain.StoryPriority;
import com.pokerplanning.story.domain.UserStory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RuleBasedStoryAnalysisService implements StoryAnalysisService {

    @Override
    public StoryAnalysis analyze(UserStory story) {
        if (story == null) {
            throw new IllegalArgumentException("UserStory must not be null");
        }
        return evaluate(
                story.getId(),
                story.getStoryKey(),
                story.getTitle(),
                story.getDescription(),
                story.getAcceptanceCriteria(),
                story.getPriority()
        );
    }

    @Override
    public StoryAnalysis analyzeText(String title, String description, List<String> acceptanceCriteria) {
        return evaluate(
                null,
                null,
                title,
                description,
                acceptanceCriteria,
                StoryPriority.MEDIUM
        );
    }

    private StoryAnalysis evaluate(
            UUID storyId,
            String storyKey,
            String title,
            String description,
            List<String> criteria,
            StoryPriority priority
    ) {
        String safeTitle = title != null ? title.trim() : "";
        String safeDesc = description != null ? description.trim() : "";
        List<String> safeCriteria = criteria != null ? criteria.stream().filter(c -> c != null && !c.isBlank()).map(String::trim).toList() : List.of();
        StoryPriority safePriority = priority != null ? priority : StoryPriority.MEDIUM;

        String combinedText = (safeTitle + " " + safeDesc + " " + String.join(" ", safeCriteria)).toLowerCase(Locale.ROOT);

        // 1. Clarity Score (0 to 100)
        int clarityScore = computeClarityScore(safeTitle, safeDesc, safeCriteria);

        // 2. Missing Acceptance Criteria Alerts
        List<String> missingAcAlerts = detectMissingAcAlerts(safeDesc, safeCriteria, combinedText);

        // 3. Technical Risk Detection
        List<String> detectedRiskFactors = detectRiskFactors(combinedText);
        RiskLevel riskLevel = determineRiskLevel(detectedRiskFactors, combinedText);

        // 4. Complexity & Fibonacci Range
        ComplexityLevel complexity = determineComplexity(safeDesc, safeCriteria, detectedRiskFactors, safePriority);
        String suggestedRange = determineEstimateRange(complexity);
        String rationale = buildEstimateRationale(complexity, detectedRiskFactors, safeCriteria.size(), safePriority);

        // 5. Clarification Questions (1 to 3 targeted questions)
        List<ClarificationQuestion> clarificationQuestions = generateClarificationQuestions(safeCriteria, detectedRiskFactors, combinedText);

        return new StoryAnalysis(
                storyId,
                storyKey,
                complexity,
                riskLevel,
                clarityScore,
                suggestedRange,
                rationale,
                missingAcAlerts,
                clarificationQuestions,
                detectedRiskFactors
        );
    }

    private int computeClarityScore(String title, String description, List<String> criteria) {
        int score = 100;

        if (title.isBlank() || title.length() < 10) {
            score -= 25;
        } else if (title.length() < 20) {
            score -= 10;
        }

        if (description.isBlank()) {
            score -= 35;
        } else if (description.length() < 30) {
            score -= 25;
        } else if (description.length() < 70) {
            score -= 10;
        }

        String lowerDesc = description.toLowerCase(Locale.ROOT);
        boolean hasUserStoryFormat = (lowerDesc.contains("as a") || lowerDesc.contains("en tant que"))
                || (lowerDesc.contains("i want") || lowerDesc.contains("je veux") || lowerDesc.contains("je souhaite"))
                || (lowerDesc.contains("so that") || lowerDesc.contains("afin de") || lowerDesc.contains("pour que"));

        if (!description.isBlank() && !hasUserStoryFormat) {
            score -= 10;
        }

        if (criteria.isEmpty()) {
            score -= 30;
        } else if (criteria.size() == 1) {
            score -= 10;
        } else if (criteria.size() >= 3) {
            score += 5;
        }

        return Math.max(10, Math.min(100, score));
    }

    private List<String> detectMissingAcAlerts(String description, List<String> criteria, String combinedText) {
        List<String> alerts = new ArrayList<>();

        if (criteria.isEmpty()) {
            alerts.add("Aucun critère d'acceptation défini. Il est fortement recommandé d'ajouter des critères testables (ex: Given / When / Then).");
        }

        boolean hasInputContext = containsAny(combinedText, "form", "formulaire", "input", "saisie", "champ", "formulaire de");
        boolean hasValidationAc = criteria.stream().anyMatch(c -> {
            String lower = c.toLowerCase(Locale.ROOT);
            return containsAny(lower, "validation", "erreur", "invalide", "obligatoire", "requis", "valide");
        });

        if (hasInputContext && !hasValidationAc && !criteria.isEmpty()) {
            alerts.add("Aucun critère ne précise les règles de validation des entrées ou la gestion des erreurs de saisie.");
        }

        boolean hasAuthContext = containsAny(combinedText, "auth", "oauth", "jwt", "role", "admin", "permission", "sécurité");
        boolean hasAuthAc = criteria.stream().anyMatch(c -> {
            String lower = c.toLowerCase(Locale.ROOT);
            return containsAny(lower, "droit", "permission", "role", "accès", "interdit", "autorisé");
        });

        if (hasAuthContext && !hasAuthAc && !criteria.isEmpty()) {
            alerts.add("Critères relatifs aux autorisations d'accès et restrictions de rôles manquants.");
        }

        return alerts;
    }

    private List<String> detectRiskFactors(String combinedText) {
        Set<String> risks = new LinkedHashSet<>();

        if (containsAny(combinedText, "auth", "oauth", "jwt", "token", "login", "password", "mot de passe", "sécurité", "security", "gdpr", "rgpd", "credential")) {
            risks.add("Sécurité & Authentification (gestion des identités / données sensibles)");
        }

        if (containsAny(combinedText, "migration", "flyway", "liquibase", "schéma", "database", "table", "sql", "index", "bdd", "base de données")) {
            risks.add("Impact persistance & migration de base de données");
        }

        if (containsAny(combinedText, "stripe", "paypal", "paiement", "payment", "webhook", "api tierce", "third-party", "partenaire", "soap")) {
            risks.add("Dépendance à une API ou service tiers externe");
        }

        if (containsAny(combinedText, "websocket", "sse", "temps réel", "real-time", "concurrence", "multithread", "asynchrone", "queue", "kafka", "rabbitmq")) {
            risks.add("Complexité temps réel / architecture asynchrone");
        }

        if (containsAny(combinedText, "suppression", "purge", "archivage", "delete", "facturation", "financial", "billing")) {
            risks.add("Opération critique sur l'intégrité des données ou flux financier");
        }

        if (containsAny(combinedText, "cache", "redis", "optimisation", "latence", "scalabilité", "benchmark", "high load", "performance")) {
            risks.add("Exigence de haute performance ou charge");
        }

        return new ArrayList<>(risks);
    }

    private RiskLevel determineRiskLevel(List<String> riskFactors, String combinedText) {
        if (riskFactors.isEmpty()) {
            return RiskLevel.LOW;
        }

        boolean hasCriticalFactor = containsAny(combinedText, "payment", "paiement", "stripe", "migration", "flyway", "delete cascade", "purge", "financial");
        if (riskFactors.size() >= 3 || hasCriticalFactor) {
            return RiskLevel.HIGH;
        }

        return RiskLevel.MEDIUM;
    }

    private ComplexityLevel determineComplexity(
            String description,
            List<String> criteria,
            List<String> riskFactors,
            StoryPriority priority
    ) {
        double score = 2.0;

        // Description length impact
        if (description.length() > 300) {
            score += 2.5;
        } else if (description.length() > 120) {
            score += 1.5;
        } else if (description.length() > 40) {
            score += 0.5;
        }

        // Criteria count impact
        int count = criteria.size();
        if (count == 0) {
            score += 1.0; // Uncertainty penalty
        } else if (count <= 2) {
            score += 1.0;
        } else if (count <= 4) {
            score += 2.0;
        } else {
            score += 3.5;
        }

        // Risks impact
        score += riskFactors.size() * 1.5;

        // Priority impact
        if (priority == StoryPriority.CRITICAL) {
            score += 2.0;
        } else if (priority == StoryPriority.HIGH) {
            score += 1.0;
        }

        if (score < 4.5) {
            return ComplexityLevel.LOW;
        } else if (score < 8.0) {
            return ComplexityLevel.MEDIUM;
        } else if (score < 12.0) {
            return ComplexityLevel.HIGH;
        } else {
            return ComplexityLevel.VERY_HIGH;
        }
    }

    private String determineEstimateRange(ComplexityLevel complexity) {
        return switch (complexity) {
            case LOW -> "1 – 2";
            case MEDIUM -> "3 – 5";
            case HIGH -> "5 – 8";
            case VERY_HIGH -> "8 – 13";
        };
    }

    private String buildEstimateRationale(ComplexityLevel complexity, List<String> risks, int criteriaCount, StoryPriority priority) {
        return switch (complexity) {
            case LOW -> "Tâche de faible envergure avec un périmètre restreint et peu de dépendances techniques.";
            case MEDIUM -> String.format("Périmètre standard de développement avec %d critère(s) d'acceptation identifié(s).", criteriaCount);
            case HIGH -> String.format("Périmètre conséquent comportant %d critères et %d facteur(s) de risque technique détecté(s).", criteriaCount, risks.size());
            case VERY_HIGH -> String.format("Complexité élevée avec %d critères et %d facteur(s) de risque. Recommandation : envisager de scinder la story.", criteriaCount, risks.size());
        };
    }

    private List<ClarificationQuestion> generateClarificationQuestions(
            List<String> criteria,
            List<String> riskFactors,
            String combinedText
    ) {
        List<ClarificationQuestion> questions = new ArrayList<>();

        if (criteria.isEmpty()) {
            questions.add(new ClarificationQuestion(
                    "Exigences",
                    "Quels sont les scénarios nominaux et alternatifs à valider pour considérer cette story comme terminée ?"
            ));
        }

        if (containsAny(combinedText, "auth", "oauth", "jwt", "login", "sécurité", "permission", "role")) {
            questions.add(new ClarificationQuestion(
                    "Sécurité",
                    "Quelles sont les permissions ou rôles précis requis pour exécuter cette action ?"
            ));
        }

        if (containsAny(combinedText, "stripe", "paypal", "paiement", "payment", "webhook", "api tierce", "third-party")) {
            questions.add(new ClarificationQuestion(
                    "Intégration",
                    "Quel est le comportement attendu et le plan de repli en cas d'indisponibilité ou d'erreur de l'API externe ?"
            ));
        }

        if (containsAny(combinedText, "migration", "flyway", "liquibase", "schéma", "database", "table")) {
            questions.add(new ClarificationQuestion(
                    "Données",
                    "Une migration de schéma ou un rétro-remplissage des données existantes est-il requis ?"
            ));
        }

        if (containsAny(combinedText, "websocket", "sse", "temps réel", "real-time", "concurrence")) {
            questions.add(new ClarificationQuestion(
                    "Temps Réel",
                    "Comment doivent être traités les conflits d'édition simultanés ou les déconnexions réseau temporaires ?"
            ));
        }

        if (questions.size() < 2) {
            questions.add(new ClarificationQuestion(
                    "UX / Ergonomie",
                    "Quels retours visuels (chargement, skeleton, notifications d'erreur) doivent être présentés à l'utilisateur ?"
            ));
        }

        if (questions.size() < 3) {
            questions.add(new ClarificationQuestion(
                    "Performance",
                    "Existe-t-il des contraintes spécifiques de temps de réponse ou de volume de données à prévoir ?"
            ));
        }

        // Limit to max 3
        return questions.stream().limit(3).toList();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (kw.contains(" ") || kw.contains("-") || kw.contains("'")) {
                if (text.contains(kw)) {
                    return true;
                }
            } else {
                // Whole word boundary matching for short tokens (like sse, sql, auth, etc.)
                String pattern = "(?i)\\b" + java.util.regex.Pattern.quote(kw) + "\\b";
                if (java.util.regex.Pattern.compile(pattern).matcher(text).find()) {
                    return true;
                }
            }
        }
        return false;
    }
}
