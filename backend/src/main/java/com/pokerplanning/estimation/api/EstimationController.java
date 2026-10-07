package com.pokerplanning.estimation.api;

import com.pokerplanning.estimation.api.dto.FinalizeEstimateRequest;
import com.pokerplanning.estimation.api.dto.StoryVotesResponse;
import com.pokerplanning.estimation.api.dto.SubmitVoteRequest;
import com.pokerplanning.estimation.application.EstimationService;
import com.pokerplanning.story.api.dto.StoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/stories/{storyId}")
@RequiredArgsConstructor
@Tag(name = "Estimation & Votes", description = "Soumission de votes à l'aveugle, révélation synchronisée, calcul de consensus et validation de l'estimation finale")
public class EstimationController {

    private final EstimationService estimationService;

    @GetMapping("/votes")
    @Operation(summary = "Obtenir les votes du tour en cours", description = "Retourne l'état des votes (masqués ou révélés) et les statistiques de consensus pour la story.")
    public ResponseEntity<StoryVotesResponse> getVotes(
        @PathVariable UUID sessionId,
        @PathVariable UUID storyId,
        @RequestParam(required = false) UUID participantId
    ) {
        return ResponseEntity.ok(estimationService.getStoryVotes(sessionId, storyId, participantId));
    }

    @PostMapping("/votes")
    @Operation(summary = "Soumettre un vote", description = "Enregistre ou modifie le vote d'un participant pour la story en cours.")
    public ResponseEntity<StoryVotesResponse> submitVote(
        @PathVariable UUID sessionId,
        @PathVariable UUID storyId,
        @Valid @RequestBody SubmitVoteRequest request
    ) {
        return ResponseEntity.ok(estimationService.submitVote(sessionId, storyId, request));
    }

    @PostMapping("/reveal")
    @Operation(summary = "Révéler les votes", description = "Dévoile simultanément tous les votes du tour et calcule automatiquement les statistiques de consensus.")
    public ResponseEntity<StoryVotesResponse> revealVotes(
        @PathVariable UUID sessionId,
        @PathVariable UUID storyId
    ) {
        return ResponseEntity.ok(estimationService.revealVotes(sessionId, storyId));
    }

    @PostMapping("/reset-votes")
    @Operation(summary = "Réinitialiser le tour de vote", description = "Efface tous les votes pour la story afin de relancer une nouvelle passe d'estimation.")
    public ResponseEntity<StoryVotesResponse> resetVotes(
        @PathVariable UUID sessionId,
        @PathVariable UUID storyId
    ) {
        return ResponseEntity.ok(estimationService.resetVotes(sessionId, storyId));
    }

    @PostMapping("/finalize")
    @Operation(summary = "Valider l'estimation finale", description = "Enregistre la complexité retenue (story points), marque la story comme ESTIMATED et la clôture.")
    public ResponseEntity<StoryResponse> finalizeEstimate(
        @PathVariable UUID sessionId,
        @PathVariable UUID storyId,
        @Valid @RequestBody FinalizeEstimateRequest request
    ) {
        return ResponseEntity.ok(estimationService.finalizeEstimate(sessionId, storyId, request));
    }
}
