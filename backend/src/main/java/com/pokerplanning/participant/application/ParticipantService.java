package com.pokerplanning.participant.application;

import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.participant.api.dto.JoinSessionRequest;
import com.pokerplanning.participant.api.dto.ParticipantResponse;
import com.pokerplanning.participant.api.dto.UpdateParticipantRoleRequest;
import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.participant.infrastructure.ParticipantRepository;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.infrastructure.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final SessionRepository sessionRepository;

    public ParticipantService(ParticipantRepository participantRepository, SessionRepository sessionRepository) {
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
    }

    public ParticipantResponse joinSession(UUID sessionId, JoinSessionRequest request) {
        PlanningSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec l'identifiant: " + sessionId));

        String trimmedName = request.name().trim();
        // Check if participant already exists in session
        Participant participant = participantRepository.findBySessionIdAndNameIgnoreCase(sessionId, trimmedName)
            .orElseGet(() -> {
                ParticipantRole role = request.role() != null ? request.role() : ParticipantRole.VOTER;
                Participant newParticipant = new Participant(session, trimmedName, request.avatar(), role);
                return participantRepository.save(newParticipant);
            });

        // Mark as online and update avatar if provided
        participant.heartbeat();
        if (request.avatar() != null && !request.avatar().isBlank()) {
            participant.setAvatar(request.avatar());
        }
        if (request.role() != null) {
            participant.setRole(request.role());
        }

        Participant saved = participantRepository.save(participant);
        return ParticipantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session introuvable avec l'identifiant: " + sessionId);
        }

        return participantRepository.findBySessionIdOrderByJoinedAtAsc(sessionId)
            .stream()
            .map(ParticipantResponse::from)
            .toList();
    }

    public ParticipantResponse updateRole(UUID sessionId, UUID participantId, UpdateParticipantRoleRequest request) {
        Participant participant = participantRepository.findByIdAndSessionId(participantId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable dans cette session."));

        participant.setRole(request.role());
        Participant saved = participantRepository.save(participant);
        return ParticipantResponse.from(saved);
    }

    public ParticipantResponse heartbeat(UUID sessionId, UUID participantId) {
        Participant participant = participantRepository.findByIdAndSessionId(participantId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable dans cette session."));

        participant.heartbeat();
        Participant saved = participantRepository.save(participant);
        return ParticipantResponse.from(saved);
    }

    public void leaveSession(UUID sessionId, UUID participantId) {
        Participant participant = participantRepository.findByIdAndSessionId(participantId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable dans cette session."));

        participant.setOnline(false);
        participantRepository.save(participant);
    }
}
