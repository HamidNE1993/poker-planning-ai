package com.pokerplanning;

import com.pokerplanning.participant.api.dto.JoinSessionRequest;
import com.pokerplanning.participant.api.dto.ParticipantResponse;
import com.pokerplanning.participant.api.dto.UpdateParticipantRoleRequest;
import com.pokerplanning.participant.application.ParticipantService;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.session.api.dto.CreateSessionRequest;
import com.pokerplanning.session.api.dto.SessionResponse;
import com.pokerplanning.session.api.dto.SessionSummaryResponse;
import com.pokerplanning.session.api.dto.UpdateSessionConfigRequest;
import com.pokerplanning.session.api.dto.UpdateSessionStatusRequest;
import com.pokerplanning.session.application.SessionService;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SessionIntegrationTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ParticipantService participantService;

    @Test
    @DisplayName("Complete end-to-end service lifecycle: create session, join participants, update config, change status")
    void shouldExecuteFullSessionAndParticipantWorkflow() {
        // 1. Create Session
        CreateSessionRequest createReq = new CreateSessionRequest(
            "Sprint 14 Poker Planning",
            "Sprint 14",
            DeckType.FIBONACCI,
            true,
            120,
            "Scrum Master John"
        );

        SessionResponse createdSession = sessionService.createSession(createReq);
        assertThat(createdSession).isNotNull();
        assertThat(createdSession.name()).isEqualTo("Sprint 14 Poker Planning");
        assertThat(createdSession.status()).isEqualTo(SessionStatus.CREATED);
        assertThat(createdSession.inviteCode()).isNotBlank();
        assertThat(createdSession.participants()).hasSize(1);
        assertThat(createdSession.participants().getFirst().name()).isEqualTo("Scrum Master John");
        assertThat(createdSession.participants().getFirst().role()).isEqualTo(ParticipantRole.FACILITATOR);

        UUID sessionId = createdSession.id();
        String inviteCode = createdSession.inviteCode();

        // 2. Fetch session by invite code
        SessionResponse fetchedByCode = sessionService.getSessionByInviteCode(inviteCode);
        assertThat(fetchedByCode.id()).isEqualTo(sessionId);

        // 3. New participant joins
        JoinSessionRequest joinReq = new JoinSessionRequest("Elena (Dev)", "avatar.png", ParticipantRole.VOTER);
        ParticipantResponse joinedParticipant = participantService.joinSession(sessionId, joinReq);
        assertThat(joinedParticipant.name()).isEqualTo("Elena (Dev)");
        assertThat(joinedParticipant.role()).isEqualTo(ParticipantRole.VOTER);
        assertThat(joinedParticipant.online()).isTrue();

        UUID participantId = joinedParticipant.id();

        // 4. Update participant role to OBSERVER
        UpdateParticipantRoleRequest roleReq = new UpdateParticipantRoleRequest(ParticipantRole.OBSERVER);
        ParticipantResponse updatedRole = participantService.updateRole(sessionId, participantId, roleReq);
        assertThat(updatedRole.role()).isEqualTo(ParticipantRole.OBSERVER);

        // 5. Update session config
        UpdateSessionConfigRequest configReq = new UpdateSessionConfigRequest(DeckType.T_SHIRT, false, 90);
        SessionResponse updatedConfig = sessionService.updateConfig(sessionId, configReq);
        assertThat(updatedConfig.deckType()).isEqualTo(DeckType.T_SHIRT);
        assertThat(updatedConfig.autoReveal()).isFalse();
        assertThat(updatedConfig.timerDurationSeconds()).isEqualTo(90);

        // 6. Update session status to IN_PROGRESS
        UpdateSessionStatusRequest statusReq = new UpdateSessionStatusRequest(SessionStatus.IN_PROGRESS);
        SessionResponse updatedStatus = sessionService.updateStatus(sessionId, statusReq);
        assertThat(updatedStatus.status()).isEqualTo(SessionStatus.IN_PROGRESS);

        // 7. List participants
        List<ParticipantResponse> participants = participantService.getParticipants(sessionId);
        assertThat(participants).hasSize(2);

        // 8. List sessions
        List<SessionSummaryResponse> allSessions = sessionService.getAllSessions(null);
        assertThat(allSessions).isNotEmpty();
    }
}
