package com.pokerplanning.estimation.infrastructure;

import com.pokerplanning.estimation.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    List<Vote> findByStoryId(UUID storyId);

    Optional<Vote> findByStoryIdAndParticipantId(UUID storyId, UUID participantId);

    void deleteByStoryId(UUID storyId);
}
