package com.pokerplanning.estimation.api.dto;

import java.util.List;
import java.util.UUID;

public record StoryVotesResponse(
    UUID storyId,
    UUID sessionId,
    boolean revealed,
    int totalVoters,
    int totalVotesReceived,
    List<VoteDetailResponse> votes,
    ConsensusStatistics consensus
) {
}
