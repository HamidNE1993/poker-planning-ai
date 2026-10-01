package com.pokerplanning.session.api.dto;

import com.pokerplanning.participant.api.dto.ParticipantResponse;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.domain.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    String name,
    String sprint,
    SessionStatus status,
    DeckType deckType,
    List<String> deckValues,
    String inviteCode,
    boolean autoReveal,
    Integer timerDurationSeconds,
    List<ParticipantResponse> participants,
    Instant createdAt,
    Instant updatedAt
) {
    public static SessionResponse from(PlanningSession session) {
        List<ParticipantResponse> participantResponses = session.getParticipants() != null
            ? session.getParticipants().stream().map(ParticipantResponse::from).toList()
            : List.of();

        return new SessionResponse(
            session.getId(),
            session.getName(),
            session.getSprint(),
            session.getStatus(),
            session.getDeckType(),
            session.getDeckType() != null ? session.getDeckType().getValues() : List.of(),
            session.getInviteCode(),
            session.isAutoReveal(),
            session.getTimerDurationSeconds(),
            participantResponses,
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }
}
