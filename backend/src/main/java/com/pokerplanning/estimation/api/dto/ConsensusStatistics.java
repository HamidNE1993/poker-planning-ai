package com.pokerplanning.estimation.api.dto;

import java.util.Map;

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
}
