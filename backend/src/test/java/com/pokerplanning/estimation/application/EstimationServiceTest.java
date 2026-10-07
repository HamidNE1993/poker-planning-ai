package com.pokerplanning.estimation.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.estimation.api.dto.ConsensusStatistics;
import com.pokerplanning.estimation.api.dto.FinalizeEstimateRequest;
import com.pokerplanning.estimation.api.dto.StoryVotesResponse;
import com.pokerplanning.estimation.api.dto.SubmitVoteRequest;
import com.pokerplanning.estimation.domain.Vote;
import com.pokerplanning.estimation.infrastructure.VoteRepository;
import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.participant.infrastructure.ParticipantRepository;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.domain.StoryPriority;
import com.pokerplanning.story.domain.StoryStatus;
import com.pokerplanning.story.domain.UserStory;
import com.pokerplanning.story.infrastructure.UserStoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstimationServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private UserStoryRepository userStoryRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private SessionEventPublisher sessionEventPublisher;

    @InjectMocks
    private EstimationService estimationService;

    private PlanningSession sampleSession;
    private UserStory sampleStory;
    private Participant sampleParticipant;
    private UUID sessionId;
    private UUID storyId;
    private UUID participantId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        participantId = UUID.randomUUID();

        sampleSession = new PlanningSession("Session 1", "Sprint 1", DeckType.FIBONACCI, "ABC456");
        sampleSession.setId(sessionId);

        sampleStory = new UserStory(sampleSession, "US-1", "Story 1", "Desc", List.of(), StoryPriority.HIGH, 0);
        sampleStory.setId(storyId);

        sampleParticipant = new Participant(sampleSession, "Alice", null, ParticipantRole.VOTER);
        sampleParticipant.setId(participantId);
    }

    @Test
    @DisplayName("Should submit vote and return votes response")
    void shouldSubmitVote() {
        SubmitVoteRequest request = new SubmitVoteRequest(participantId, "5");

        when(userStoryRepository.findByIdAndSessionId(storyId, sessionId)).thenReturn(Optional.of(sampleStory));
        when(participantRepository.findByIdAndSessionId(participantId, sessionId)).thenReturn(Optional.of(sampleParticipant));
        when(voteRepository.findByStoryIdAndParticipantId(storyId, participantId)).thenReturn(Optional.empty());
        when(participantRepository.findBySessionId(sessionId)).thenReturn(List.of(sampleParticipant));
        when(voteRepository.findByStoryId(storyId)).thenReturn(List.of(new Vote(sampleStory, sampleParticipant, "5")));

        StoryVotesResponse response = estimationService.submitVote(sessionId, storyId, request);

        assertThat(response).isNotNull();
        assertThat(response.storyId()).isEqualTo(storyId);
        assertThat(response.totalVotesReceived()).isEqualTo(1);
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    @DisplayName("Should calculate consensus statistics accurately for numeric votes")
    void shouldCalculateConsensusAccurately() {
        Participant p1 = new Participant(sampleSession, "Alice", null, ParticipantRole.VOTER);
        Participant p2 = new Participant(sampleSession, "Bob", null, ParticipantRole.VOTER);
        Participant p3 = new Participant(sampleSession, "Charlie", null, ParticipantRole.VOTER);

        List<Vote> votes = List.of(
            new Vote(sampleStory, p1, "5"),
            new Vote(sampleStory, p2, "5"),
            new Vote(sampleStory, p3, "8")
        );

        ConsensusStatistics stats = estimationService.calculateConsensus(votes, 3);

        assertThat(stats).isNotNull();
        assertThat(stats.average()).isEqualTo(6.0);
        assertThat(stats.median()).isEqualTo("5");
        assertThat(stats.minVote()).isEqualTo("5");
        assertThat(stats.maxVote()).isEqualTo("8");
        assertThat(stats.consensusAgreementPercent()).isEqualTo(67);
        assertThat(stats.unanimous()).isFalse();
    }

    @Test
    @DisplayName("Should finalize estimate on user story")
    void shouldFinalizeEstimate() {
        FinalizeEstimateRequest request = new FinalizeEstimateRequest("5");

        when(userStoryRepository.findByIdAndSessionId(storyId, sessionId)).thenReturn(Optional.of(sampleStory));
        when(userStoryRepository.save(any(UserStory.class))).thenAnswer(i -> i.getArgument(0));

        StoryResponse response = estimationService.finalizeEstimate(sessionId, storyId, request);

        assertThat(response.finalEstimate()).isEqualTo("5");
        assertThat(response.status()).isEqualTo(StoryStatus.ESTIMATED);
        assertThat(response.votesRevealed()).isTrue();
    }
}
