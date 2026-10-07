package com.pokerplanning.participant.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.participant.api.dto.JoinSessionRequest;
import com.pokerplanning.participant.api.dto.ParticipantResponse;
import com.pokerplanning.participant.api.dto.UpdateParticipantRoleRequest;
import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.participant.infrastructure.ParticipantRepository;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.infrastructure.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionEventPublisher sessionEventPublisher;

    @InjectMocks
    private ParticipantService participantService;

    private PlanningSession sampleSession;
    private UUID sessionId;
    private UUID participantId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        participantId = UUID.randomUUID();
        sampleSession = new PlanningSession("Sprint 12", "Sprint 12", DeckType.FIBONACCI, "INV123");
        sampleSession.setId(sessionId);
    }

    @Test
    @DisplayName("Should join new participant to session successfully")
    void shouldJoinNewParticipant() {
        JoinSessionRequest request = new JoinSessionRequest("Elena Rostova", "https://avatar.url", ParticipantRole.VOTER);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(sampleSession));
        when(participantRepository.findBySessionIdAndNameIgnoreCase(sessionId, "Elena Rostova")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant p = invocation.getArgument(0);
            p.setId(participantId);
            return p;
        });

        ParticipantResponse response = participantService.joinSession(sessionId, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Elena Rostova");
        assertThat(response.role()).isEqualTo(ParticipantRole.VOTER);
        assertThat(response.online()).isTrue();
        verify(participantRepository, atLeastOnce()).save(any(Participant.class));
    }

    @Test
    @DisplayName("Should update participant role")
    void shouldUpdateParticipantRole() {
        Participant participant = new Participant(sampleSession, "Alex", null, ParticipantRole.VOTER);
        participant.setId(participantId);

        when(participantRepository.findByIdAndSessionId(participantId, sessionId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantResponse response = participantService.updateRole(
            sessionId,
            participantId,
            new UpdateParticipantRoleRequest(ParticipantRole.OBSERVER)
        );

        assertThat(response.role()).isEqualTo(ParticipantRole.OBSERVER);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when participant not found for heartbeat")
    void shouldThrowWhenParticipantNotFoundForHeartbeat() {
        when(participantRepository.findByIdAndSessionId(participantId, sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.heartbeat(sessionId, participantId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should mark participant offline when leaving session")
    void shouldMarkParticipantOfflineWhenLeaving() {
        Participant participant = new Participant(sampleSession, "Alex", null, ParticipantRole.VOTER);
        participant.setId(participantId);

        when(participantRepository.findByIdAndSessionId(participantId, sessionId)).thenReturn(Optional.of(participant));

        participantService.leaveSession(sessionId, participantId);

        assertThat(participant.isOnline()).isFalse();
        verify(participantRepository).save(participant);
    }
}
