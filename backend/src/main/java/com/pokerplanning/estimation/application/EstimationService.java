package com.pokerplanning.estimation.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.collaboration.domain.SessionEventType;
import com.pokerplanning.common.exception.BusinessRuleException;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.estimation.api.dto.*;
import com.pokerplanning.estimation.domain.Vote;
import com.pokerplanning.estimation.infrastructure.VoteRepository;
import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.participant.infrastructure.ParticipantRepository;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.infrastructure.SessionRepository;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.domain.StoryStatus;
import com.pokerplanning.story.domain.UserStory;
import com.pokerplanning.story.infrastructure.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EstimationService {

    private final VoteRepository voteRepository;
    private final UserStoryRepository userStoryRepository;
    private final ParticipantRepository participantRepository;
    private final SessionRepository sessionRepository;
    private final SessionEventPublisher sessionEventPublisher;

    public StoryVotesResponse submitVote(UUID sessionId, UUID storyId, SubmitVoteRequest request) {
        log.info("Vote soumis pour participant {} sur la story {} de la session {}", request.participantId(), storyId, sessionId);
        UserStory story = findStoryOrThrow(sessionId, storyId);
        Participant participant = participantRepository.findByIdAndSessionId(request.participantId(), sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant non trouvé dans cette session : " + request.participantId()));

        if (participant.getRole() == ParticipantRole.OBSERVER) {
            throw new BusinessRuleException("Les observateurs ne peuvent pas participer au vote.");
        }

        // If story status is not VOTING, set it to VOTING
        if (story.getStatus() == StoryStatus.PENDING) {
            story.setStatus(StoryStatus.VOTING);
        }

        Optional<Vote> existingVote = voteRepository.findByStoryIdAndParticipantId(storyId, participant.getId());
        Vote vote;
        if (existingVote.isPresent()) {
            vote = existingVote.get();
            vote.setVoteValue(request.voteValue());
            vote.setVotedAt(java.time.Instant.now());
        } else {
            vote = new Vote(story, participant, request.voteValue());
        }
        voteRepository.save(vote);

        // Check auto-reveal if enabled on session
        PlanningSession session = story.getSession();
        if (session.isAutoReveal()) {
            long voterCount = participantRepository.findBySessionId(sessionId).stream()
                .filter(p -> p.getRole() != ParticipantRole.OBSERVER && p.isOnline())
                .count();
            long voteCount = voteRepository.findByStoryId(storyId).size();
            if (voteCount >= voterCount && voterCount > 0) {
                story.setVotesRevealed(true);
            }
        }

        userStoryRepository.save(story);

        StoryVotesResponse maskedBroadcastResponse = buildVotesResponse(story, null);
        if (story.isVotesRevealed()) {
            sessionEventPublisher.publish(sessionId, SessionEventType.VOTES_REVEALED, maskedBroadcastResponse);
        } else {
            sessionEventPublisher.publish(sessionId, SessionEventType.VOTE_SUBMITTED, maskedBroadcastResponse);
        }

        return buildVotesResponse(story, request.participantId());
    }

    @Transactional(readOnly = true)
    public StoryVotesResponse getStoryVotes(UUID sessionId, UUID storyId, UUID requestingParticipantId) {
        UserStory story = findStoryOrThrow(sessionId, storyId);
        return buildVotesResponse(story, requestingParticipantId);
    }

    public StoryVotesResponse revealVotes(UUID sessionId, UUID storyId) {
        log.info("Révélation des votes pour la story {} de la session {}", storyId, sessionId);
        UserStory story = findStoryOrThrow(sessionId, storyId);
        story.setVotesRevealed(true);
        userStoryRepository.save(story);
        StoryVotesResponse response = buildVotesResponse(story, null);
        sessionEventPublisher.publish(sessionId, SessionEventType.VOTES_REVEALED, response);
        return response;
    }

    public StoryVotesResponse resetVotes(UUID sessionId, UUID storyId) {
        log.info("Réinitialisation des votes pour la story {} de la session {}", storyId, sessionId);
        UserStory story = findStoryOrThrow(sessionId, storyId);
        voteRepository.deleteByStoryId(storyId);
        story.setVotesRevealed(false);
        userStoryRepository.save(story);
        StoryVotesResponse response = buildVotesResponse(story, null);
        sessionEventPublisher.publish(sessionId, SessionEventType.VOTES_RESET, response);
        return response;
    }

    public StoryResponse finalizeEstimate(UUID sessionId, UUID storyId, FinalizeEstimateRequest request) {
        log.info("Finalisation de l'estimation pour la story {} : {} pts", storyId, request.finalEstimate());
        UserStory story = findStoryOrThrow(sessionId, storyId);
        story.setFinalEstimate(request.finalEstimate());
        story.setStatus(StoryStatus.ESTIMATED);
        story.setVotesRevealed(true);

        UserStory saved = userStoryRepository.save(story);
        StoryResponse response = StoryResponse.fromEntity(saved);
        sessionEventPublisher.publish(sessionId, SessionEventType.ESTIMATE_FINALIZED, response);
        return response;
    }

    private StoryVotesResponse buildVotesResponse(UserStory story, UUID requestingParticipantId) {
        UUID sessionId = story.getSession().getId();
        List<Participant> allParticipants = participantRepository.findBySessionId(sessionId);
        List<Vote> votes = voteRepository.findByStoryId(story.getId());
        Map<UUID, Vote> votesByParticipantId = votes.stream()
            .collect(Collectors.toMap(v -> v.getParticipant().getId(), v -> v));

        boolean revealed = story.isVotesRevealed();

        List<VoteDetailResponse> voteDetails = allParticipants.stream().map(p -> {
            Vote vote = votesByParticipantId.get(p.getId());
            boolean hasVoted = vote != null;
            String visibleValue = null;

            if (hasVoted) {
                if (revealed || (requestingParticipantId != null && requestingParticipantId.equals(p.getId()))) {
                    visibleValue = vote.getVoteValue();
                }
            }

            return new VoteDetailResponse(
                p.getId(),
                p.getName(),
                p.getRole(),
                visibleValue,
                hasVoted,
                vote != null ? vote.getVotedAt() : null
            );
        }).toList();

        int totalVoters = (int) allParticipants.stream()
            .filter(p -> p.getRole() != ParticipantRole.OBSERVER)
            .count();
        int totalVotesReceived = votes.size();

        ConsensusStatistics consensus = null;
        if (revealed && !votes.isEmpty()) {
            consensus = calculateConsensus(votes, totalVoters);
        }

        return new StoryVotesResponse(
            story.getId(),
            sessionId,
            revealed,
            totalVoters,
            totalVotesReceived,
            voteDetails,
            consensus
        );
    }

    ConsensusStatistics calculateConsensus(List<Vote> votes, int totalVoters) {
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

        // Try parsing numeric votes
        List<Double> numericValues = new ArrayList<>();
        for (String val : rawValues) {
            try {
                numericValues.add(Double.parseDouble(val));
            } catch (NumberFormatException ignored) {
            }
        }

        Double average = null;
        String minVote = null;
        String maxVote = null;
        String median = null;

        if (!numericValues.isEmpty() && numericValues.size() == rawValues.size()) {
            Collections.sort(numericValues);
            double sum = 0;
            for (Double n : numericValues) {
                sum += n;
            }
            double avg = sum / numericValues.size();
            average = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();

            int size = numericValues.size();
            double med;
            if (size % 2 == 1) {
                med = numericValues.get(size / 2);
            } else {
                med = (numericValues.get(size / 2 - 1) + numericValues.get(size / 2)) / 2.0;
            }
            median = med % 1 == 0 ? String.valueOf((int) med) : String.valueOf(med);
            minVote = numericValues.getFirst() % 1 == 0 ? String.valueOf(numericValues.getFirst().intValue()) : String.valueOf(numericValues.getFirst());
            maxVote = numericValues.getLast() % 1 == 0 ? String.valueOf(numericValues.getLast().intValue()) : String.valueOf(numericValues.getLast());
        } else {
            // Categorical or mixed votes
            minVote = rawValues.getFirst();
            maxVote = rawValues.getLast();
            // Mode as median fallback
            median = distribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(rawValues.getFirst());
        }

        // Agreement percent calculation
        int maxVoteCount = distribution.values().stream().max(Integer::compareTo).orElse(1);
        int agreementPercent = (int) Math.round(((double) maxVoteCount / rawValues.size()) * 100);

        boolean unanimous = distribution.size() == 1;
        String consensusLabel;
        if (unanimous) {
            consensusLabel = "Consensus Unanime (100%)";
        } else if (agreementPercent >= 70) {
            consensusLabel = "Fort Consensus (" + agreementPercent + "%)";
        } else if (agreementPercent >= 50) {
            consensusLabel = "Consensus Modéré (" + agreementPercent + "%)";
        } else {
            consensusLabel = "Divergence (" + agreementPercent + "%)";
        }

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

    private UserStory findStoryOrThrow(UUID sessionId, UUID storyId) {
        return userStoryRepository.findByIdAndSessionId(storyId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Story non trouvée : " + storyId));
    }
}
