package com.pokerplanning.estimation.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.collaboration.domain.SessionEventType;
import com.pokerplanning.common.exception.BusinessRuleException;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.estimation.api.dto.*;
import com.pokerplanning.estimation.domain.ConsensusCalculator;
import com.pokerplanning.estimation.domain.ConsensusStatistics;
import com.pokerplanning.estimation.domain.Vote;
import com.pokerplanning.estimation.infrastructure.VoteRepository;
import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.participant.infrastructure.ParticipantRepository;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.infrastructure.SessionRepository;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.domain.UserStory;
import com.pokerplanning.story.infrastructure.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service orchestrating the estimation use cases and voting lifecycle.
 */
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
    private final ConsensusCalculator consensusCalculator;

    public StoryVotesResponse submitVote(UUID sessionId, UUID storyId, SubmitVoteRequest request) {
        log.info("Vote soumis pour participant {} sur la story {} de la session {}", request.participantId(), storyId, sessionId);
        UserStory story = findStoryOrThrow(sessionId, storyId);
        Participant participant = participantRepository.findByIdAndSessionId(request.participantId(), sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant non trouvé dans cette session : " + request.participantId()));

        if (!participant.canVote()) {
            throw new BusinessRuleException("Les observateurs ne peuvent pas participer au vote.");
        }

        // Activate voting if not yet active
        if (!story.isVoting() && !story.isEstimated()) {
            story.startVoting();
        }

        Optional<Vote> existingVote = voteRepository.findByStoryIdAndParticipantId(storyId, participant.getId());
        Vote vote;
        if (existingVote.isPresent()) {
            vote = existingVote.get();
            vote.changeVote(request.voteValue());
        } else {
            vote = new Vote(story, participant, request.voteValue());
        }
        voteRepository.save(vote);

        // Check auto-reveal if enabled on session
        PlanningSession session = story.getSession();
        if (session.isAutoReveal()) {
            long voterCount = participantRepository.findBySessionId(sessionId).stream()
                .filter(Participant::canVote)
                .count();
            long voteCount = voteRepository.findByStoryId(storyId).size();
            if (voteCount >= voterCount && voterCount > 0) {
                story.revealVotes();
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
        story.revealVotes();
        userStoryRepository.save(story);
        StoryVotesResponse response = buildVotesResponse(story, null);
        sessionEventPublisher.publish(sessionId, SessionEventType.VOTES_REVEALED, response);
        return response;
    }

    public StoryVotesResponse resetVotes(UUID sessionId, UUID storyId) {
        log.info("Réinitialisation des votes pour la story {} de la session {}", storyId, sessionId);
        UserStory story = findStoryOrThrow(sessionId, storyId);
        voteRepository.deleteByStoryId(storyId);
        story.hideVotes();
        userStoryRepository.save(story);
        StoryVotesResponse response = buildVotesResponse(story, null);
        sessionEventPublisher.publish(sessionId, SessionEventType.VOTES_RESET, response);
        return response;
    }

    public StoryResponse finalizeEstimate(UUID sessionId, UUID storyId, FinalizeEstimateRequest request) {
        log.info("Finalisation de l'estimation pour la story {} : {} pts", storyId, request.finalEstimate());
        UserStory story = findStoryOrThrow(sessionId, storyId);
        story.finalizeEstimate(request.finalEstimate());

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

            if (hasVoted && (revealed || (requestingParticipantId != null && requestingParticipantId.equals(p.getId())))) {
                visibleValue = vote.getVoteValue();
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
            .filter(Participant::canVote)
            .count();
        int totalVotesReceived = votes.size();

        ConsensusStatistics consensus = null;
        if (revealed && !votes.isEmpty()) {
            consensus = consensusCalculator.calculate(votes, totalVoters);
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

    private UserStory findStoryOrThrow(UUID sessionId, UUID storyId) {
        return userStoryRepository.findByIdAndSessionId(storyId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Story non trouvée : " + storyId));
    }
}
