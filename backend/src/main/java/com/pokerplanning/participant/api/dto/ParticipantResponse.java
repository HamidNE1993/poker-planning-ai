package com.pokerplanning.participant.api.dto;

import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.participant.domain.ParticipantRole;

import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
    UUID id,
    UUID sessionId,
    String name,
    String avatar,
    ParticipantRole role,
    boolean online,
    Instant joinedAt,
    Instant lastHeartbeatAt
) {
    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(
            participant.getId(),
            participant.getSession() != null ? participant.getSession().getId() : null,
            participant.getName(),
            participant.getAvatar(),
            participant.getRole(),
            participant.isOnline(),
            participant.getJoinedAt(),
            participant.getLastHeartbeatAt()
        );
    }
}
