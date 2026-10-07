package com.pokerplanning.ai.application;

import com.pokerplanning.ai.domain.ComplexityLevel;
import com.pokerplanning.ai.domain.RiskLevel;
import com.pokerplanning.ai.domain.StoryAnalysis;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.story.domain.StoryPriority;
import com.pokerplanning.story.domain.UserStory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedStoryAnalysisServiceTest {

    private RuleBasedStoryAnalysisService service;
    private PlanningSession session;

    @BeforeEach
    void setUp() {
        service = new RuleBasedStoryAnalysisService();
        session = new PlanningSession("Sprint 1", "Sprint 1", DeckType.FIBONACCI, "INV-12345");
    }

    @Test
    @DisplayName("Should analyze well-structured story with high clarity score and low complexity")
    void testWellStructuredStory() {
        UserStory story = new UserStory(
                session,
                "US-101",
                "Affichage du profil utilisateur",
                "En tant qu'utilisateur, je veux consulter mes informations personnelles afin de vérifier mes coordonnées.",
                List.of(
                        "Afficher le nom et prénom",
                        "Afficher l'adresse email",
                        "Afficher la date d'inscription",
                        "Design responsive sur mobile"
                ),
                StoryPriority.LOW,
                0
        );

        StoryAnalysis analysis = service.analyze(story);

        assertThat(analysis).isNotNull();
        assertThat(analysis.storyKey()).isEqualTo("US-101");
        assertThat(analysis.clarityScore()).isGreaterThanOrEqualTo(80);
        assertThat(analysis.complexity()).isIn(ComplexityLevel.LOW, ComplexityLevel.MEDIUM);
        assertThat(analysis.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(analysis.missingAcceptanceCriteriaAlerts()).isEmpty();
        assertThat(analysis.clarificationQuestions()).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should flag incomplete and ambiguous story with missing AC alert")
    void testIncompleteStory() {
        UserStory story = new UserStory(
                session,
                "US-102",
                "Fix bouton",
                "",
                List.of(),
                StoryPriority.MEDIUM,
                1
        );

        StoryAnalysis analysis = service.analyze(story);

        assertThat(analysis).isNotNull();
        assertThat(analysis.clarityScore()).isLessThan(50);
        assertThat(analysis.missingAcceptanceCriteriaAlerts()).isNotEmpty();
        assertThat(analysis.missingAcceptanceCriteriaAlerts().get(0)).contains("Aucun critère d'acceptation");
        assertThat(analysis.clarificationQuestions()).anyMatch(q -> q.category().equals("Exigences"));
    }

    @Test
    @DisplayName("Should detect security and database migration risks")
    void testRiskDetection() {
        UserStory story = new UserStory(
                session,
                "US-103",
                "Authentification OAuth2 et migration de la table utilisateurs",
                "En tant qu'utilisateur, je souhaite me connecter via OAuth2 avec JWT token et nous devons exécuter un script de migration Flyway sur la table users.",
                List.of(
                        "Gérer le flux OAuth2 Google",
                        "Valider le JWT et les permissions",
                        "Script Flyway rétro-compatible"
                ),
                StoryPriority.HIGH,
                2
        );

        StoryAnalysis analysis = service.analyze(story);

        assertThat(analysis.detectedRiskFactors()).isNotEmpty();
        assertThat(analysis.detectedRiskFactors()).anyMatch(r -> r.contains("Sécurité") || r.contains("migration"));
        assertThat(analysis.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(analysis.clarificationQuestions()).anyMatch(q -> q.category().equals("Sécurité") || q.category().equals("Données"));
    }

    @Test
    @DisplayName("Should evaluate text-only story analysis without UserStory entity")
    void testAnalyzeText() {
        StoryAnalysis analysis = service.analyzeText(
                "Intégration du webhook de paiement Stripe",
                "En tant que client, je veux payer par carte bancaire avec Stripe.",
                List.of("Gérer le webhook checkout.session.completed", "Gérer les erreurs de paiement")
        );

        assertThat(analysis).isNotNull();
        assertThat(analysis.storyId()).isNull();
        assertThat(analysis.storyKey()).isNull();
        assertThat(analysis.detectedRiskFactors()).anyMatch(r -> r.contains("tiers") || r.contains("API"));
        assertThat(analysis.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(analysis.clarificationQuestions()).anyMatch(q -> q.category().equals("Intégration"));
    }

    @Test
    @DisplayName("Should throw exception when UserStory is null")
    void testNullStory() {
        assertThatThrownBy(() -> service.analyze(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
