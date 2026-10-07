package com.pokerplanning.story.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.session.domain.DeckType;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.infrastructure.SessionRepository;
import com.pokerplanning.story.api.dto.CreateStoryRequest;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.api.dto.UpdateStoryRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStoryServiceTest {

    @Mock
    private UserStoryRepository userStoryRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionEventPublisher sessionEventPublisher;

    @InjectMocks
    private UserStoryService userStoryService;

    private PlanningSession sampleSession;
    private UUID sessionId;
    private UUID storyId;
    private UserStory sampleStory;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        sampleSession = new PlanningSession("Session 1", "Sprint 1", DeckType.FIBONACCI, "INV123");
        sampleSession.setId(sessionId);

        sampleStory = new UserStory(sampleSession, "US-1", "Auth Feature", "Login desc", List.of("AC1"), StoryPriority.HIGH, 0);
        sampleStory.setId(storyId);
    }

    @Test
    @DisplayName("Should create story successfully")
    void shouldCreateStory() {
        CreateStoryRequest request = new CreateStoryRequest("US-1", "Auth Feature", "Login desc", List.of("AC1"), StoryPriority.HIGH);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(sampleSession));
        when(userStoryRepository.countBySessionId(sessionId)).thenReturn(0L);
        when(userStoryRepository.save(any(UserStory.class))).thenAnswer(i -> {
            UserStory s = i.getArgument(0);
            s.setId(storyId);
            return s;
        });

        StoryResponse response = userStoryService.createStory(sessionId, request);

        assertThat(response).isNotNull();
        assertThat(response.storyKey()).isEqualTo("US-1");
        assertThat(response.title()).isEqualTo("Auth Feature");
        assertThat(response.status()).isEqualTo(StoryStatus.VOTING); // first story marked VOTING
        verify(userStoryRepository).save(any(UserStory.class));
    }

    @Test
    @DisplayName("Should update story")
    void shouldUpdateStory() {
        UpdateStoryRequest request = new UpdateStoryRequest("US-1-UPDATED", "Updated Title", "Updated desc", List.of("AC1", "AC2"), StoryPriority.CRITICAL, StoryStatus.ESTIMATED, "8");

        when(userStoryRepository.findByIdAndSessionId(storyId, sessionId)).thenReturn(Optional.of(sampleStory));

        StoryResponse response = userStoryService.updateStory(sessionId, storyId, request);

        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(response.storyKey()).isEqualTo("US-1-UPDATED");
        assertThat(response.priority()).isEqualTo(StoryPriority.CRITICAL);
        assertThat(response.status()).isEqualTo(StoryStatus.ESTIMATED);
        assertThat(response.finalEstimate()).isEqualTo("8");
    }

    @Test
    @DisplayName("Should select active story and reset other voting stories")
    void shouldSelectActiveStory() {
        UserStory secondStory = new UserStory(sampleSession, "US-2", "Payment Feature", "Pay desc", List.of(), StoryPriority.MEDIUM, 1);
        UUID secondId = UUID.randomUUID();
        secondStory.setId(secondId);

        sampleStory.setStatus(StoryStatus.VOTING);

        when(sessionRepository.existsById(sessionId)).thenReturn(true);
        when(userStoryRepository.findBySessionIdOrderByOrderIndexAsc(sessionId)).thenReturn(List.of(sampleStory, secondStory));

        StoryResponse response = userStoryService.selectActiveStory(sessionId, secondId);

        assertThat(response.id()).isEqualTo(secondId);
        assertThat(response.status()).isEqualTo(StoryStatus.VOTING);
        assertThat(sampleStory.getStatus()).isEqualTo(StoryStatus.PENDING);
    }
}
