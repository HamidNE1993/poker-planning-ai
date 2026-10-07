package com.pokerplanning.estimation.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Domain Service calculating consensus and statistical distributions for story voting rounds.
 */
@Component
public class ConsensusCalculator {

    public ConsensusStatistics calculate(List<Vote> votes, int totalVoters) {
        if (votes == null || votes.isEmpty()) {
            return new ConsensusStatistics(null, null, "Aucun vote", 0, null, null, 0, totalVoters, false, Map.of());
        }

        List<String> rawValues = votes.stream()
            .map(Vote::getVoteValue)
            .filter(v -> v != null && !v.isBlank())
            .toList();

        if (rawValues.isEmpty()) {
            return new ConsensusStatistics(null, null, "Aucun vote", 0, null, null, 0, totalVoters, false, Map.of());
        }

        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (String val : rawValues) {
            distribution.put(val, distribution.getOrDefault(val, 0) + 1);
        }

        List<Double> numericValues = new ArrayList<>();
        for (String val : rawValues) {
            try {
                numericValues.add(Double.parseDouble(val));
            } catch (NumberFormatException ignored) {
                // Ignore non-numeric votes for mathematical average/median
            }
        }

        Double average = null;
        String minVote;
        String maxVote;
        String median;

        if (!numericValues.isEmpty() && numericValues.size() == rawValues.size()) {
            Collections.sort(numericValues);
            double sum = numericValues.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / numericValues.size();
            average = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();

            int size = numericValues.size();
            double med;
            if (size % 2 == 1) {
                med = numericValues.get(size / 2);
            } else {
                med = (numericValues.get(size / 2 - 1) + numericValues.get(size / 2)) / 2.0;
            }
            median = formatNumeric(med);
            minVote = formatNumeric(numericValues.getFirst());
            maxVote = formatNumeric(numericValues.getLast());
        } else {
            minVote = rawValues.getFirst();
            maxVote = rawValues.getLast();
            median = distribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(rawValues.getFirst());
        }

        int maxVoteCount = distribution.values().stream().max(Integer::compareTo).orElse(1);
        int agreementPercent = (int) Math.round(((double) maxVoteCount / rawValues.size()) * 100);
        boolean unanimous = distribution.size() == 1;

        String consensusLabel = determineConsensusLabel(unanimous, agreementPercent);

        return new ConsensusStatistics(
            average,
            median,
            consensusLabel,
            agreementPercent,
            minVote,
            maxVote,
            rawValues.size(),
            totalVoters,
            unanimous,
            distribution
        );
    }

    private String formatNumeric(double value) {
        return value % 1 == 0 ? String.valueOf((int) value) : String.valueOf(value);
    }

    private String determineConsensusLabel(boolean unanimous, int agreementPercent) {
        if (unanimous) {
            return "Consensus Unanime (100%)";
        }
        if (agreementPercent >= 70) {
            return "Fort Consensus (" + agreementPercent + "%)";
        }
        if (agreementPercent >= 50) {
            return "Consensus Modéré (" + agreementPercent + "%)";
        }
        return "Divergence (" + agreementPercent + "%)";
    }
}
