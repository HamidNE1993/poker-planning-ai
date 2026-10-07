package com.pokerplanning.collaboration.api;

import com.pokerplanning.collaboration.api.dto.TimerActionRequest;
import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.collaboration.domain.SessionEventType;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.session.infrastructure.SessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Collaboration Temps Réel", description = "Endpoints de synchronisation temps réel par Server-Sent Events (SSE)")
public class SessionCollaborationController {

    private final SessionEventPublisher sessionEventPublisher;
    private final SessionRepository sessionRepository;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "S'abonner au flux d'événements temps réel de la session")
    public SseEmitter subscribeToSessionEvents(@PathVariable UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session non trouvée : " + sessionId);
        }
        return sessionEventPublisher.subscribe(sessionId);
    }

    @PostMapping("/timer")
    @Operation(summary = "Synchroniser une action sur le timer pour tous les participants")
    public ResponseEntity<Void> synchronizeTimer(
        @PathVariable UUID sessionId,
        @Valid @RequestBody TimerActionRequest request
    ) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session non trouvée : " + sessionId);
        }

        log.info("Action timer reçue pour la session {}: {}", sessionId, request.action());
        sessionEventPublisher.publish(sessionId, SessionEventType.TIMER_SYNC, request);
        return ResponseEntity.ok().build();
    }
}
