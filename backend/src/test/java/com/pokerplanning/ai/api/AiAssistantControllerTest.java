package com.pokerplanning.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokerplanning.ai.api.dto.AnalyzeTextRequest;
import com.pokerplanning.common.exception.GlobalExceptionHandler;
import com.pokerplanning.session.api.dto.CreateSessionRequest;
import com.pokerplanning.session.api.dto.SessionResponse;
import com.pokerplanning.session.application.SessionService;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.story.api.dto.CreateStoryRequest;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.application.UserStoryService;
import com.pokerplanning.story.domain.StoryPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AiAssistantControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AiAssistantController aiAssistantController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserStoryService userStoryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiAssistantController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/stories/{storyId}/analysis - Should return analysis for existing story")
    void testGetStoryAnalysis() throws Exception {
        SessionResponse session = sessionService.createSession(new CreateSessionRequest(
                "Sprint IA Test",
                "Sprint 1",
                DeckType.FIBONACCI,
                false,
                60,
                "Scrum Master"
        ));

        StoryResponse story = userStoryService.createStory(session.id(), new CreateStoryRequest(
                "US-AI-1",
                "Authentification JWT et connexion OAuth2",
                "En tant qu'utilisateur, je veux me connecter avec mon token JWT.",
                List.of("Gérer le token d'accès", "Gérer le refresh token"),
                StoryPriority.HIGH
        ));

        mockMvc.perform(get("/api/stories/{storyId}/analysis", story.id())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storyId").value(story.id().toString()))
                .andExpect(jsonPath("$.storyKey").value("US-AI-1"))
                .andExpect(jsonPath("$.clarityScore").isNumber())
                .andExpect(jsonPath("$.complexity").isNotEmpty())
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.suggestedEstimateRange").isNotEmpty())
                .andExpect(jsonPath("$.estimateRationale").isNotEmpty())
                .andExpect(jsonPath("$.clarificationQuestions", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.detectedRiskFactors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/stories/{storyId}/analysis - Should return 404 for non-existing story")
    void testGetStoryAnalysisNotFound() throws Exception {
        mockMvc.perform(get("/api/stories/{storyId}/analysis", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/ai/analyze - Should analyze ad-hoc text input successfully")
    void testAnalyzeAdHocText() throws Exception {
        AnalyzeTextRequest request = new AnalyzeTextRequest(
                "Migration du schéma de données vers PostgreSQL",
                "En tant que DBA, je veux migrer les tables avec Flyway.",
                List.of("Script de migration validé", "Rollback testé")
        );

        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clarityScore").isNumber())
                .andExpect(jsonPath("$.detectedRiskFactors", hasItem(containsString("migration"))))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"));
    }

    @Test
    @DisplayName("POST /api/ai/analyze - Should return 400 when title is blank")
    void testAnalyzeAdHocTextBlankTitle() throws Exception {
        AnalyzeTextRequest request = new AnalyzeTextRequest(
                "   ",
                "Description sans titre",
                List.of()
        );

        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
