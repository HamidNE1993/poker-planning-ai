package com.pokerplanning.session.api;

import com.pokerplanning.session.api.dto.CreateSessionRequest;
import com.pokerplanning.session.api.dto.SessionResponse;
import com.pokerplanning.session.api.dto.SessionSummaryResponse;
import com.pokerplanning.session.api.dto.UpdateSessionConfigRequest;
import com.pokerplanning.session.api.dto.UpdateSessionStatusRequest;
import com.pokerplanning.session.application.SessionService;
import com.pokerplanning.session.domain.SessionStatus;
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
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions de Planning", description = "Gestion du cycle de vie et configuration des sessions de Planning Poker")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "Créer une nouvelle session", description = "Initialise une session de planning poker avec son facilitateur et génère un code d'invitation unique.")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionResponse session = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping
    @Operation(summary = "Lister les sessions", description = "Récupère la liste synthétique des sessions avec filtrage optionnel par statut.")
    public ResponseEntity<List<SessionSummaryResponse>> getAllSessions(
        @RequestParam(required = false) SessionStatus status
    ) {
        List<SessionSummaryResponse> sessions = sessionService.getAllSessions(status);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une session par ID ou code", description = "Récupère les détails complets d'une session par son UUID ou son code d'invitation alphanumérique.")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable String id) {
        SessionResponse session = sessionService.getSessionByIdOrCode(id);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/code/{inviteCode}")
    @Operation(summary = "Obtenir une session par code d'invitation", description = "Recherche et retourne une session à partir de son code court alphanumérique.")
    public ResponseEntity<SessionResponse> getSessionByInviteCode(@PathVariable String inviteCode) {
        SessionResponse session = sessionService.getSessionByInviteCode(inviteCode);
        return ResponseEntity.ok(session);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Mettre à jour le statut d'une session", description = "Change l'état de la session (CREATED, IN_PROGRESS, COMPLETED).")
    public ResponseEntity<SessionResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSessionStatusRequest request
    ) {
        SessionResponse session = sessionService.updateStatus(id, request);
        return ResponseEntity.ok(session);
    }

    @PatchMapping("/{id}/config")
    @Operation(summary = "Mettre à jour la configuration", description = "Modifie le système de vote, le compte à rebours ou l'auto-révélation.")
    public ResponseEntity<SessionResponse> updateConfig(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSessionConfigRequest request
    ) {
        SessionResponse session = sessionService.updateConfig(id, request);
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une session", description = "Supprime définitivement une session et toutes ses données associées.")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
