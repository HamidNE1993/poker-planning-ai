package com.pokerplanning.story.api.dto;

import com.pokerplanning.story.domain.StoryPriority;
import com.pokerplanning.story.domain.StoryStatus;
import com.pokerplanning.story.domain.UserStory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoryResponse(
    UUID id,
    UUID sessionId,
    String storyKey,
    String title,
    String description,
    List<String> acceptanceCriteria,
    StoryPriority priority,
    StoryStatus status,
    String finalEstimate,
    Integer orderIndex,
    boolean votesRevealed,
    Instant createdAt
) {
    public static StoryResponse fromEntity(UserStory story) {
        return new StoryResponse(
            story.getId(),
            story.getSession().getId(),
            story.getStoryKey(),
            story.getTitle(),
            story.getDescription(),
            story.getAcceptanceCriteria(),
            story.getPriority(),
            story.getStatus(),
            story.getFinalEstimate(),
            story.getOrderIndex(),
            story.isVotesRevealed(),
            story.getCreatedAt()
        );
    }
}
