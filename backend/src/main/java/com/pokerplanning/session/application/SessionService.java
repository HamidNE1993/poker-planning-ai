package com.pokerplanning.session.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.collaboration.domain.SessionEventType;
import com.pokerplanning.common.exception.BusinessRuleException;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.session.api.dto.CreateSessionRequest;
import com.pokerplanning.session.api.dto.SessionResponse;
import com.pokerplanning.session.api.dto.SessionSummaryResponse;
import com.pokerplanning.session.api.dto.UpdateSessionConfigRequest;
import com.pokerplanning.session.api.dto.UpdateSessionStatusRequest;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.domain.SessionStatus;
import com.pokerplanning.session.infrastructure.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private final SessionRepository sessionRepository;
    private final SessionEventPublisher sessionEventPublisher;

    public SessionResponse createSession(CreateSessionRequest request) {
        log.info("Création d'une nouvelle session de planning: '{}' (Sprint: {})", request.name(), request.sprint());
        String inviteCode = generateUniqueInviteCode();
        DeckType deckType = request.deckType() != null ? request.deckType() : DeckType.FIBONACCI;

        PlanningSession session = new PlanningSession(
            request.name(),
            request.sprint(),
            deckType,
            inviteCode
        );

        if (request.autoReveal() != null) {
            session.setAutoReveal(request.autoReveal());
        }
        if (request.timerDurationSeconds() != null) {
            session.setTimerDurationSeconds(request.timerDurationSeconds());
        }

        // Add creator as Facilitator participant
        Participant facilitator = new Participant(
            session,
            request.facilitatorName().trim(),
            null,
            ParticipantRole.FACILITATOR
        );
        session.addParticipant(facilitator);

        PlanningSession saved = sessionRepository.save(session);
        return SessionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getAllSessions(SessionStatus status) {
        List<PlanningSession> sessions = status != null
            ? sessionRepository.findByStatusOrderByCreatedAtDesc(status)
            : sessionRepository.findAllByOrderByCreatedAtDesc();

        return sessions.stream().map(SessionSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(UUID id) {
        PlanningSession session = sessionRepository.findByIdWithParticipants(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec l'identifiant: " + id));

        return SessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionByIdOrCode(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Identifiant de session invalide");
        }
        String cleaned = identifier.trim();
        if (cleaned.toUpperCase().startsWith("PKR-")) {
            cleaned = cleaned.substring(4);
        }

        try {
            UUID uuid = UUID.fromString(cleaned);
            var sessionOpt = sessionRepository.findByIdWithParticipants(uuid);
            if (sessionOpt.isPresent()) {
                return SessionResponse.from(sessionOpt.get());
            }
        } catch (IllegalArgumentException ignored) {
            // Pas un UUID valide, recherche par code d'invitation ci-dessous
        }

        return getSessionByInviteCode(cleaned);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionByInviteCode(String inviteCode) {
        String code = inviteCode != null ? inviteCode.toUpperCase().trim() : "";
        if (code.startsWith("PKR-")) {
            code = code.substring(4);
        }
        PlanningSession session = sessionRepository.findByInviteCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec le code: " + inviteCode));

        return SessionResponse.from(session);
    }

    public SessionResponse updateStatus(UUID id, UpdateSessionStatusRequest request) {
        PlanningSession session = sessionRepository.findByIdWithParticipants(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec l'identifiant: " + id));

        session.changeStatus(request.status());
        PlanningSession saved = sessionRepository.save(session);
        SessionResponse response = SessionResponse.from(saved);
        sessionEventPublisher.publish(id, SessionEventType.SESSION_UPDATED, response);
        return response;
    }

    public SessionResponse updateConfig(UUID id, UpdateSessionConfigRequest request) {
        PlanningSession session = sessionRepository.findByIdWithParticipants(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec l'identifiant: " + id));

        try {
            session.updateConfiguration(request.deckType(), request.autoReveal(), request.timerDurationSeconds());
        } catch (IllegalStateException e) {
            throw new BusinessRuleException(e.getMessage());
        }

        PlanningSession saved = sessionRepository.save(session);
        SessionResponse response = SessionResponse.from(saved);
        sessionEventPublisher.publish(id, SessionEventType.SESSION_UPDATED, response);
        return response;
    }

    public void deleteSession(UUID id) {
        if (!sessionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Session introuvable avec l'identifiant: " + id);
        }
        sessionRepository.deleteById(id);
    }

    private String generateUniqueInviteCode() {
        for (int attempts = 0; attempts < 10; attempts++) {
            StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                code.append(INVITE_CHARS.charAt(random.nextInt(INVITE_CHARS.length())));
            }
            String generated = code.toString();
            if (!sessionRepository.existsByInviteCode(generated)) {
                return generated;
            }
        }
        return UUID.randomUUID().toString().substring(0, INVITE_CODE_LENGTH).toUpperCase();
    }
}
