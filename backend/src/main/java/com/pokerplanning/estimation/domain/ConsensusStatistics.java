package com.pokerplanning.estimation.domain;

import java.util.Map;

/**
 * Value Object representing the consensus and statistical distribution of votes for an estimated user story.
 */
public record ConsensusStatistics(
    Double average,
    String median,
    String consensusLabel,
    int consensusAgreementPercent,
    String minVote,
    String maxVote,
    int totalVotes,
    int totalVoters,
    boolean unanimous,
    Map<String, Integer> distribution
) {
    public ConsensusStatistics {
        distribution = distribution != null ? Map.copyOf(distribution) : Map.of();
    }
}
