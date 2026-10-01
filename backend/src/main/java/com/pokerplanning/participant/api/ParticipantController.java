package com.pokerplanning.participant.api;

import com.pokerplanning.participant.api.dto.JoinSessionRequest;
import com.pokerplanning.participant.api.dto.ParticipantResponse;
import com.pokerplanning.participant.api.dto.UpdateParticipantRoleRequest;
import com.pokerplanning.participant.application.ParticipantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    public ResponseEntity<ParticipantResponse> joinSession(
        @PathVariable UUID sessionId,
        @Valid @RequestBody JoinSessionRequest request
    ) {
        ParticipantResponse participant = participantService.joinSession(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(participant);
    }

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable UUID sessionId) {
        List<ParticipantResponse> participants = participantService.getParticipants(sessionId);
        return ResponseEntity.ok(participants);
    }

    @PatchMapping("/{participantId}/role")
    public ResponseEntity<ParticipantResponse> updateRole(
        @PathVariable UUID sessionId,
        @PathVariable UUID participantId,
        @Valid @RequestBody UpdateParticipantRoleRequest request
    ) {
        ParticipantResponse participant = participantService.updateRole(sessionId, participantId, request);
        return ResponseEntity.ok(participant);
    }

    @PostMapping("/{participantId}/heartbeat")
    public ResponseEntity<ParticipantResponse> heartbeat(
        @PathVariable UUID sessionId,
        @PathVariable UUID participantId
    ) {
        ParticipantResponse participant = participantService.heartbeat(sessionId, participantId);
        return ResponseEntity.ok(participant);
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> leaveSession(
        @PathVariable UUID sessionId,
        @PathVariable UUID participantId
    ) {
        participantService.leaveSession(sessionId, participantId);
        return ResponseEntity.noContent().build();
    }
}
