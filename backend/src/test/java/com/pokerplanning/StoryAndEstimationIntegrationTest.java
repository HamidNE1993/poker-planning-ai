package com.pokerplanning;

import com.pokerplanning.estimation.api.dto.FinalizeEstimateRequest;
import com.pokerplanning.estimation.api.dto.StoryVotesResponse;
import com.pokerplanning.estimation.api.dto.SubmitVoteRequest;
import com.pokerplanning.estimation.application.EstimationService;
import com.pokerplanning.participant.api.dto.JoinSessionRequest;
import com.pokerplanning.participant.api.dto.ParticipantResponse;
import com.pokerplanning.participant.application.ParticipantService;
import com.pokerplanning.participant.domain.ParticipantRole;
import com.pokerplanning.session.api.dto.CreateSessionRequest;
import com.pokerplanning.session.api.dto.SessionResponse;
import com.pokerplanning.session.application.SessionService;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.story.api.dto.CreateStoryRequest;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.application.UserStoryService;
import com.pokerplanning.story.domain.StoryPriority;
import com.pokerplanning.story.domain.StoryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StoryAndEstimationIntegrationTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private UserStoryService userStoryService;

    @Autowired
    private EstimationService estimationService;

    @Test
    @DisplayName("Complete workflow: create session, add stories, join voters, vote, calculate consensus, finalize estimate")
    void shouldExecuteFullStoryAndEstimationWorkflow() {
        // 1. Create Session
        CreateSessionRequest sessionReq = new CreateSessionRequest(
            "Sprint 15 Planning",
            "Sprint 15",
            DeckType.FIBONACCI,
            false,
            60,
            "Sarah (Facilitator)"
        );
        SessionResponse session = sessionService.createSession(sessionReq);
        UUID sessionId = session.id();
        UUID facilitatorId = session.participants().getFirst().id();

        // 2. Add two user stories
        CreateStoryRequest story1Req = new CreateStoryRequest(
            "US-101",
            "Implémentation OAuth2",
            "Description auth",
            List.of("AC1", "AC2"),
            StoryPriority.HIGH
        );
        StoryResponse story1 = userStoryService.createStory(sessionId, story1Req);
        assertThat(story1.status()).isEqualTo(StoryStatus.VOTING);

        CreateStoryRequest story2Req = new CreateStoryRequest(
            "US-102",
            "Export PDF rapport",
            "Description PDF",
            List.of("AC1"),
            StoryPriority.MEDIUM
        );
        StoryResponse story2 = userStoryService.createStory(sessionId, story2Req);
        assertThat(story2.status()).isEqualTo(StoryStatus.PENDING);

        // 3. Join two voters
        ParticipantResponse voter1 = participantService.joinSession(sessionId, new JoinSessionRequest("Bob", null, ParticipantRole.VOTER));
        ParticipantResponse voter2 = participantService.joinSession(sessionId, new JoinSessionRequest("Alice", null, ParticipantRole.VOTER));

        // 4. Submit votes for Story 1
        estimationService.submitVote(sessionId, story1.id(), new SubmitVoteRequest(voter1.id(), "5"));
        estimationService.submitVote(sessionId, story1.id(), new SubmitVoteRequest(voter2.id(), "5"));
        StoryVotesResponse votesBeforeReveal = estimationService.submitVote(sessionId, story1.id(), new SubmitVoteRequest(facilitatorId, "8"));

        assertThat(votesBeforeReveal.revealed()).isFalse();
        assertThat(votesBeforeReveal.totalVotesReceived()).isEqualTo(3);

        // 5. Reveal votes & verify consensus
        StoryVotesResponse revealed = estimationService.revealVotes(sessionId, story1.id());
        assertThat(revealed.revealed()).isTrue();
        assertThat(revealed.consensus()).isNotNull();
        assertThat(revealed.consensus().average()).isEqualTo(6.0);
        assertThat(revealed.consensus().median()).isEqualTo("5");
        assertThat(revealed.consensus().consensusAgreementPercent()).isEqualTo(67);

        // 6. Finalize estimate on Story 1
        StoryResponse finalizedStory = estimationService.finalizeEstimate(sessionId, story1.id(), new FinalizeEstimateRequest("5"));
        assertThat(finalizedStory.status()).isEqualTo(StoryStatus.ESTIMATED);
        assertThat(finalizedStory.finalEstimate()).isEqualTo("5");

        // 7. Select and activate Story 2 for voting
        StoryResponse activatedStory2 = userStoryService.selectActiveStory(sessionId, story2.id());
        assertThat(activatedStory2.status()).isEqualTo(StoryStatus.VOTING);

        // 8. Verify story list reflects final state
        List<StoryResponse> allStories = userStoryService.getStoriesForSession(sessionId);
        assertThat(allStories).hasSize(2);
        assertThat(allStories.get(0).status()).isEqualTo(StoryStatus.ESTIMATED);
        assertThat(allStories.get(1).status()).isEqualTo(StoryStatus.VOTING);
    }
}
