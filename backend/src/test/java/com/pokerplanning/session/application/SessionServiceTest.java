package com.pokerplanning.session.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.common.exception.BusinessRuleException;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.session.api.dto.CreateSessionRequest;
import com.pokerplanning.session.api.dto.SessionResponse;
import com.pokerplanning.session.api.dto.SessionSummaryResponse;
import com.pokerplanning.session.api.dto.UpdateSessionConfigRequest;
import com.pokerplanning.session.api.dto.UpdateSessionStatusRequest;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.domain.SessionStatus;
import com.pokerplanning.session.infrastructure.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionEventPublisher sessionEventPublisher;

    @InjectMocks
    private SessionService sessionService;

    private PlanningSession sampleSession;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        sampleSession = new PlanningSession("Sprint 12 Planning", "Sprint 12", DeckType.FIBONACCI, "ABC123");
        sampleSession.setId(sessionId);
    }

    @Test
    @DisplayName("Should create session with facilitator as initial participant")
    void shouldCreateSessionWithFacilitator() {
        CreateSessionRequest request = new CreateSessionRequest(
            "Sprint 12 Planning",
            "Sprint 12",
            DeckType.FIBONACCI,
            true,
            90,
            "Sarah Connor"
        );

        when(sessionRepository.existsByInviteCode(any())).thenReturn(false);
        when(sessionRepository.save(any(PlanningSession.class))).thenAnswer(invocation -> {
            PlanningSession toSave = invocation.getArgument(0);
            toSave.setId(sessionId);
            return toSave;
        });

        SessionResponse response = sessionService.createSession(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Sprint 12 Planning");
        assertThat(response.sprint()).isEqualTo("Sprint 12");
        assertThat(response.deckType()).isEqualTo(DeckType.FIBONACCI);
        assertThat(response.autoReveal()).isTrue();
        assertThat(response.timerDurationSeconds()).isEqualTo(90);
        assertThat(response.participants()).hasSize(1);
        assertThat(response.participants().getFirst().name()).isEqualTo("Sarah Connor");
        verify(sessionRepository).save(any(PlanningSession.class));
    }

    @Test
    @DisplayName("Should get session by ID when exists")
    void shouldGetSessionById() {
        when(sessionRepository.findByIdWithParticipants(sessionId)).thenReturn(Optional.of(sampleSession));

        SessionResponse response = sessionService.getSessionById(sessionId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(sessionId);
        assertThat(response.name()).isEqualTo("Sprint 12 Planning");
    }

    @Test
    @DisplayName("Should get session by identifier with invite code or UUID")
    void shouldGetSessionByIdOrCode() {
        when(sessionRepository.findByIdWithParticipants(sessionId)).thenReturn(Optional.of(sampleSession));
        when(sessionRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(sampleSession));

        SessionResponse resByUuid = sessionService.getSessionByIdOrCode(sessionId.toString());
        assertThat(resByUuid.id()).isEqualTo(sessionId);

        SessionResponse resByCode = sessionService.getSessionByIdOrCode("ABC123");
        assertThat(resByCode.id()).isEqualTo(sessionId);

        SessionResponse resByPrefixedCode = sessionService.getSessionByIdOrCode("PKR-ABC123");
        assertThat(resByPrefixedCode.id()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when session not found by ID")
    void shouldThrowWhenSessionNotFoundById() {
        when(sessionRepository.findByIdWithParticipants(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSessionById(sessionId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Session introuvable");
    }

    @Test
    @DisplayName("Should update session status")
    void shouldUpdateSessionStatus() {
        when(sessionRepository.findByIdWithParticipants(sessionId)).thenReturn(Optional.of(sampleSession));
        when(sessionRepository.save(any(PlanningSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.updateStatus(
            sessionId,
            new UpdateSessionStatusRequest(SessionStatus.IN_PROGRESS)
        );

        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        verify(sessionRepository).save(sampleSession);
    }

    @Test
    @DisplayName("Should prevent updating configuration when session is completed")
    void shouldThrowWhenConfiguringCompletedSession() {
        sampleSession.setStatus(SessionStatus.COMPLETED);
        when(sessionRepository.findByIdWithParticipants(sessionId)).thenReturn(Optional.of(sampleSession));

        UpdateSessionConfigRequest configRequest = new UpdateSessionConfigRequest(DeckType.T_SHIRT, true, 120);

        assertThatThrownBy(() -> sessionService.updateConfig(sessionId, configRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("terminée");
    }

    @Test
    @DisplayName("Should delete session when exists")
    void shouldDeleteSession() {
        when(sessionRepository.existsById(sessionId)).thenReturn(true);

        sessionService.deleteSession(sessionId);

        verify(sessionRepository).deleteById(sessionId);
    }
}
