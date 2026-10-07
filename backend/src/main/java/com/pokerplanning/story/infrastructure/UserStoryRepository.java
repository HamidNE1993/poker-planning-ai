package com.pokerplanning.story.infrastructure;

import com.pokerplanning.story.domain.StoryStatus;
import com.pokerplanning.story.domain.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, UUID> {

    List<UserStory> findBySessionIdOrderByOrderIndexAsc(UUID sessionId);

    Optional<UserStory> findByIdAndSessionId(UUID id, UUID sessionId);

    Optional<UserStory> findFirstBySessionIdAndStatus(UUID sessionId, StoryStatus status);

    long countBySessionId(UUID sessionId);
}
