package com.pokerplanning.story.api;

import com.pokerplanning.story.api.dto.CreateStoryRequest;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.api.dto.UpdateStoryRequest;
import com.pokerplanning.story.application.UserStoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/stories")
@RequiredArgsConstructor
@Tag(name = "User Stories & Backlog", description = "Gestion du backlog de user stories, critères d'acceptation et sélection de story active")
public class UserStoryController {

    private final UserStoryService userStoryService;

    @GetMapping
    @Operation(summary = "Lister les user stories", description = "Récupère toutes les user stories associées à une session ordonnées par index de passage.")
    public ResponseEntity<List<StoryResponse>> getStories(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(userStoryService.getStoriesForSession(sessionId));
    }

    @GetMapping("/{storyId}")
    @Operation(summary = "Obtenir une user story", description = "Récupère les détails complets d'une user story.")
    public ResponseEntity<StoryResponse> getStory(@PathVariable UUID sessionId, @PathVariable UUID storyId) {
        return ResponseEntity.ok(userStoryService.getStory(sessionId, storyId));
    }

    @PostMapping
    @Operation(summary = "Créer une user story", description = "Ajoute une nouvelle user story dans la file d'attente de la session.")
    public ResponseEntity<StoryResponse> createStory(
        @PathVariable UUID sessionId,
        @Valid @RequestBody CreateStoryRequest request
    ) {
        StoryResponse created = userStoryService.createStory(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{storyId}")
    @Operation(summary = "Mettre à jour une user story", description = "Modifie le titre, la description, les critères d'acceptation ou le statut d'une story.")
    public ResponseEntity<StoryResponse> updateStory(
        @PathVariable UUID sessionId,
        @PathVariable UUID storyId,
        @Valid @RequestBody UpdateStoryRequest request
    ) {
        return ResponseEntity.ok(userStoryService.updateStory(sessionId, storyId, request));
    }

    @DeleteMapping("/{storyId}")
    @Operation(summary = "Supprimer une user story", description = "Supprime une user story de la session.")
    public ResponseEntity<Void> deleteStory(@PathVariable UUID sessionId, @PathVariable UUID storyId) {
        userStoryService.deleteStory(sessionId, storyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{storyId}/select")
    @Operation(summary = "Activer une user story", description = "Définit la user story sélectionnée comme la story active en cours d'estimation (statut VOTING).")
    public ResponseEntity<StoryResponse> selectActiveStory(
        @PathVariable UUID sessionId,
        @PathVariable UUID storyId
    ) {
        return ResponseEntity.ok(userStoryService.selectActiveStory(sessionId, storyId));
    }
}
