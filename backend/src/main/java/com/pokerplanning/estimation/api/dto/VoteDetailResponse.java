package com.pokerplanning.estimation.api.dto;

import com.pokerplanning.participant.domain.ParticipantRole;

import java.time.Instant;
import java.util.UUID;

public record VoteDetailResponse(
    UUID participantId,
    String participantName,
    ParticipantRole participantRole,
    String voteValue,
    boolean hasVoted,
    Instant votedAt
) {
}
