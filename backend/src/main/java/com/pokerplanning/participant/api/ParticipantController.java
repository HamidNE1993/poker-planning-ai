package com.pokerplanning.participant.api;

import com.pokerplanning.participant.api.dto.JoinSessionRequest;
import com.pokerplanning.participant.api.dto.ParticipantResponse;
import com.pokerplanning.participant.api.dto.UpdateParticipantRoleRequest;
import com.pokerplanning.participant.application.ParticipantService;
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
@RequestMapping("/api/sessions/{sessionId}/participants")
@RequiredArgsConstructor
@Tag(name = "Participants", description = "Gestion des participants, rôles et suivi de présence en temps réel")
public class ParticipantController {

    private final ParticipantService participantService;

    @PostMapping
    @Operation(summary = "Rejoindre une session", description = "Enregistre ou reconnecte un participant dans une session avec son rôle et pseudo.")
    public ResponseEntity<ParticipantResponse> joinSession(
        @PathVariable UUID sessionId,
        @Valid @RequestBody JoinSessionRequest request
    ) {
        ParticipantResponse participant = participantService.joinSession(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(participant);
    }

    @GetMapping
    @Operation(summary = "Lister les participants d'une session", description = "Récupère tous les participants de la session ordonnés par date d'arrivée.")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable UUID sessionId) {
        List<ParticipantResponse> participants = participantService.getParticipants(sessionId);
        return ResponseEntity.ok(participants);
    }

    @PatchMapping("/{participantId}/role")
    @Operation(summary = "Modifier le rôle d'un participant", description = "Permet de basculer entre Facilitateur, Votant ou Observateur.")
    public ResponseEntity<ParticipantResponse> updateRole(
        @PathVariable UUID sessionId,
        @PathVariable UUID participantId,
        @Valid @RequestBody UpdateParticipantRoleRequest request
    ) {
        ParticipantResponse participant = participantService.updateRole(sessionId, participantId, request);
        return ResponseEntity.ok(participant);
    }

    @PostMapping("/{participantId}/heartbeat")
    @Operation(summary = "Signaler la présence active (heartbeat)", description = "Actualise l'horodatage d'activité et marque le participant comme en ligne.")
    public ResponseEntity<ParticipantResponse> heartbeat(
        @PathVariable UUID sessionId,
        @PathVariable UUID participantId
    ) {
        ParticipantResponse participant = participantService.heartbeat(sessionId, participantId);
        return ResponseEntity.ok(participant);
    }

    @PostMapping("/{participantId}/leave")
    @Operation(summary = "Quitter la session", description = "Déclare le participant comme déconnecté / hors ligne.")
    public ResponseEntity<Void> leaveSession(
        @PathVariable UUID sessionId,
        @PathVariable UUID participantId
    ) {
        participantService.leaveSession(sessionId, participantId);
        return ResponseEntity.noContent().build();
    }
}
