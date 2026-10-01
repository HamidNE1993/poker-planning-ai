package com.pokerplanning.session.application;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SessionService {

    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public SessionResponse createSession(CreateSessionRequest request) {
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
    public SessionResponse getSessionByInviteCode(String inviteCode) {
        PlanningSession session = sessionRepository.findByInviteCode(inviteCode.toUpperCase().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec le code: " + inviteCode));

        return SessionResponse.from(session);
    }

    public SessionResponse updateStatus(UUID id, UpdateSessionStatusRequest request) {
        PlanningSession session = sessionRepository.findByIdWithParticipants(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec l'identifiant: " + id));

        session.setStatus(request.status());
        PlanningSession saved = sessionRepository.save(session);
        return SessionResponse.from(saved);
    }

    public SessionResponse updateConfig(UUID id, UpdateSessionConfigRequest request) {
        PlanningSession session = sessionRepository.findByIdWithParticipants(id)
            .orElseThrow(() -> new ResourceNotFoundException("Session introuvable avec l'identifiant: " + id));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("Impossible de modifier la configuration d'une session terminée.");
        }

        if (request.deckType() != null) {
            session.setDeckType(request.deckType());
        }
        if (request.autoReveal() != null) {
            session.setAutoReveal(request.autoReveal());
        }
        if (request.timerDurationSeconds() != null) {
            session.setTimerDurationSeconds(request.timerDurationSeconds());
        }

        PlanningSession saved = sessionRepository.save(session);
        return SessionResponse.from(saved);
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
