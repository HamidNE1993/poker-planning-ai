package com.pokerplanning;

import com.pokerplanning.collaboration.api.dto.TimerActionRequest;
import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.collaboration.domain.SessionEventType;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.infrastructure.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionCollaborationIntegrationTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionEventPublisher sessionEventPublisher;

    private PlanningSession session;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        session = new PlanningSession("Sprint 42 Planning", "Sprint 42", DeckType.FIBONACCI, "REAL77");
        session = sessionRepository.save(session);
    }

    @Test
    @DisplayName("Should allow client to subscribe to SSE session stream and publish events")
    void shouldSubscribeAndPublishEvents() {
        UUID sessionId = session.getId();

        SseEmitter emitter = sessionEventPublisher.subscribe(sessionId);
        assertThat(emitter).isNotNull();

        assertDoesNotThrow(() -> {
            sessionEventPublisher.publish(sessionId, SessionEventType.SESSION_UPDATED, "Session refreshed");
            sessionEventPublisher.publish(sessionId, SessionEventType.TIMER_SYNC, new TimerActionRequest(TimerActionRequest.TimerAction.START, 60));
        });
    }
}
