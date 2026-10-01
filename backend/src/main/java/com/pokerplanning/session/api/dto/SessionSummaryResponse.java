package com.pokerplanning.session.api.dto;

import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.domain.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionSummaryResponse(
    UUID id,
    String name,
    String sprint,
    SessionStatus status,
    DeckType deckType,
    String inviteCode,
    boolean autoReveal,
    Integer timerDurationSeconds,
    int participantCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static SessionSummaryResponse from(PlanningSession session) {
        return new SessionSummaryResponse(
            session.getId(),
            session.getName(),
            session.getSprint(),
            session.getStatus(),
            session.getDeckType(),
            session.getInviteCode(),
            session.isAutoReveal(),
            session.getTimerDurationSeconds(),
            session.getParticipants() != null ? session.getParticipants().size() : 0,
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }
}
