package com.pokerplanning.session.api;

import com.pokerplanning.session.api.dto.CreateSessionRequest;
import com.pokerplanning.session.api.dto.SessionResponse;
import com.pokerplanning.session.api.dto.SessionSummaryResponse;
import com.pokerplanning.session.api.dto.UpdateSessionConfigRequest;
import com.pokerplanning.session.api.dto.UpdateSessionStatusRequest;
import com.pokerplanning.session.application.SessionService;
import com.pokerplanning.session.domain.SessionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionResponse session = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping
    public ResponseEntity<List<SessionSummaryResponse>> getAllSessions(
        @RequestParam(required = false) SessionStatus status
    ) {
        List<SessionSummaryResponse> sessions = sessionService.getAllSessions(status);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable UUID id) {
        SessionResponse session = sessionService.getSessionById(id);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/code/{inviteCode}")
    public ResponseEntity<SessionResponse> getSessionByInviteCode(@PathVariable String inviteCode) {
        SessionResponse session = sessionService.getSessionByInviteCode(inviteCode);
        return ResponseEntity.ok(session);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SessionResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSessionStatusRequest request
    ) {
        SessionResponse session = sessionService.updateStatus(id, request);
        return ResponseEntity.ok(session);
    }

    @PatchMapping("/{id}/config")
    public ResponseEntity<SessionResponse> updateConfig(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSessionConfigRequest request
    ) {
        SessionResponse session = sessionService.updateConfig(id, request);
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
