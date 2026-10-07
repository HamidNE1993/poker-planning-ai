package com.pokerplanning.participant.infrastructure;

import com.pokerplanning.participant.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findBySessionId(UUID sessionId);

    List<Participant> findBySessionIdOrderByJoinedAtAsc(UUID sessionId);

    Optional<Participant> findByIdAndSessionId(UUID id, UUID sessionId);

    Optional<Participant> findBySessionIdAndNameIgnoreCase(UUID sessionId, String name);

    boolean existsBySessionIdAndNameIgnoreCase(UUID sessionId, String name);
}
