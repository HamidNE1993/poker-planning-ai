package com.pokerplanning.ai.api;

import com.pokerplanning.ai.api.dto.AnalyzeTextRequest;
import com.pokerplanning.ai.api.dto.StoryAnalysisResponse;
import com.pokerplanning.ai.application.StoryAnalysisService;
import com.pokerplanning.ai.domain.StoryAnalysis;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.story.domain.UserStory;
import com.pokerplanning.story.infrastructure.UserStoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Story Assistant", description = "Analyse heuristique et intelligente de User Stories (détection d'ambiguïté, risques, suggestions de questions et estimations)")
public class AiAssistantController {

    private final StoryAnalysisService storyAnalysisService;
    private final UserStoryRepository userStoryRepository;

    @GetMapping("/api/stories/{storyId}/analysis")
    @Operation(summary = "Analyser une User Story", description = "Génère l'analyse complète (clarté, critères manquants, risques, questions de cadrage, fourchette indicative) pour une story existante.")
    public ResponseEntity<StoryAnalysisResponse> analyzeStory(@PathVariable UUID storyId) {
        log.info("Demande d'analyse IA pour la story {}", storyId);
        UserStory story = userStoryRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("User story non trouvée avec l'id : " + storyId));

        StoryAnalysis analysis = storyAnalysisService.analyze(story);
        return ResponseEntity.ok(StoryAnalysisResponse.fromDomain(analysis));
    }

    @GetMapping("/api/sessions/{sessionId}/stories/{storyId}/analysis")
    @Operation(summary = "Analyser une User Story dans une session", description = "Génère l'analyse d'une user story liée à une session spécifique.")
    public ResponseEntity<StoryAnalysisResponse> analyzeStoryInSession(
            @PathVariable UUID sessionId,
            @PathVariable UUID storyId
    ) {
        log.info("Demande d'analyse IA pour la story {} dans la session {}", storyId, sessionId);
        UserStory story = userStoryRepository.findByIdAndSessionId(storyId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("User story non trouvée dans la session : " + storyId));

        StoryAnalysis analysis = storyAnalysisService.analyze(story);
        return ResponseEntity.ok(StoryAnalysisResponse.fromDomain(analysis));
    }

    @PostMapping("/api/ai/analyze")
    @Operation(summary = "Analyser un texte de story ad-hoc", description = "Permet de tester l'analyse IA sur un texte libre (titre, description, critères) sans enregistrement préalable en base.")
    public ResponseEntity<StoryAnalysisResponse> analyzeAdHocText(@Valid @RequestBody AnalyzeTextRequest request) {
        log.info("Demande d'analyse IA ad-hoc pour le titre '{}'", request.title());
        StoryAnalysis analysis = storyAnalysisService.analyzeText(
                request.title(),
                request.description(),
                request.acceptanceCriteria()
        );
        return ResponseEntity.ok(StoryAnalysisResponse.fromDomain(analysis));
    }
}
