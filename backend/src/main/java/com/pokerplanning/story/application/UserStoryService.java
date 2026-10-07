package com.pokerplanning.story.application;

import com.pokerplanning.collaboration.application.SessionEventPublisher;
import com.pokerplanning.collaboration.domain.SessionEventType;
import com.pokerplanning.common.exception.ResourceNotFoundException;
import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.infrastructure.SessionRepository;
import com.pokerplanning.story.api.dto.CreateStoryRequest;
import com.pokerplanning.story.api.dto.StoryResponse;
import com.pokerplanning.story.api.dto.UpdateStoryRequest;
import com.pokerplanning.story.domain.StoryStatus;
import com.pokerplanning.story.domain.UserStory;
import com.pokerplanning.story.infrastructure.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;
    private final SessionRepository sessionRepository;
    private final SessionEventPublisher sessionEventPublisher;

    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesForSession(UUID sessionId) {
        ensureSessionExists(sessionId);
        return userStoryRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
            .map(StoryResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public StoryResponse getStory(UUID sessionId, UUID storyId) {
        UserStory story = findStoryOrThrow(sessionId, storyId);
        return StoryResponse.fromEntity(story);
    }

    public StoryResponse createStory(UUID sessionId, CreateStoryRequest request) {
        log.info("Création d'une user story pour la session {}: '{}'", sessionId, request.title());
        PlanningSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée : " + sessionId));

        long count = userStoryRepository.countBySessionId(sessionId);
        String storyKey = request.storyKey();
        if (storyKey == null || storyKey.isBlank()) {
            storyKey = "US-" + (count + 1);
        }

        UserStory story = new UserStory(
            session,
            storyKey,
            request.title(),
            request.description(),
            request.acceptanceCriteria(),
            request.priority(),
            (int) count
        );

        // If it's the very first story, mark it as VOTING by default
        if (count == 0) {
            story.setStatus(StoryStatus.VOTING);
        }

        UserStory saved = userStoryRepository.save(story);
        StoryResponse response = StoryResponse.fromEntity(saved);
        sessionEventPublisher.publish(sessionId, SessionEventType.STORY_CREATED, response);
        return response;
    }

    public StoryResponse updateStory(UUID sessionId, UUID storyId, UpdateStoryRequest request) {
        UserStory story = findStoryOrThrow(sessionId, storyId);

        if (request.storyKey() != null && !request.storyKey().isBlank()) {
            story.setStoryKey(request.storyKey());
        }
        story.setTitle(request.title());
        story.setDescription(request.description());
        if (request.acceptanceCriteria() != null) {
            story.setAcceptanceCriteria(request.acceptanceCriteria());
        }
        if (request.priority() != null) {
            story.setPriority(request.priority());
        }
        if (request.status() != null) {
            story.setStatus(request.status());
        }
        if (request.finalEstimate() != null) {
            story.setFinalEstimate(request.finalEstimate());
        }

        StoryResponse response = StoryResponse.fromEntity(story);
        sessionEventPublisher.publish(sessionId, SessionEventType.STORY_UPDATED, response);
        return response;
    }

    public void deleteStory(UUID sessionId, UUID storyId) {
        UserStory story = findStoryOrThrow(sessionId, storyId);
        userStoryRepository.delete(story);
        sessionEventPublisher.publish(sessionId, SessionEventType.STORY_DELETED, storyId);
    }

    public StoryResponse selectActiveStory(UUID sessionId, UUID storyId) {
        ensureSessionExists(sessionId);

        List<UserStory> stories = userStoryRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
        UserStory targetStory = null;

        for (UserStory s : stories) {
            if (s.getId().equals(storyId)) {
                s.setStatus(StoryStatus.VOTING);
                targetStory = s;
            } else if (s.getStatus() == StoryStatus.VOTING) {
                // If it was voting but not completed, revert to pending
                s.setStatus(StoryStatus.PENDING);
            }
        }

        if (targetStory == null) {
            throw new ResourceNotFoundException("Story non trouvée dans la session : " + storyId);
        }

        StoryResponse response = StoryResponse.fromEntity(targetStory);
        sessionEventPublisher.publish(sessionId, SessionEventType.STORY_SELECTED, response);
        return response;
    }

    private UserStory findStoryOrThrow(UUID sessionId, UUID storyId) {
        return userStoryRepository.findByIdAndSessionId(storyId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Story non trouvée avec l'id : " + storyId));
    }

    private void ensureSessionExists(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session non trouvée : " + sessionId);
        }
    }
}
