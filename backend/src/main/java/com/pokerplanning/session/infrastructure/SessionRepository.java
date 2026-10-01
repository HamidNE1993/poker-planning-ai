package com.pokerplanning.session.infrastructure;

import com.pokerplanning.session.domain.PlanningSession;
import com.pokerplanning.session.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<PlanningSession, UUID> {

    Optional<PlanningSession> findByInviteCode(String inviteCode);

    List<PlanningSession> findByStatusOrderByCreatedAtDesc(SessionStatus status);

    List<PlanningSession> findAllByOrderByCreatedAtDesc();

    boolean existsByInviteCode(String inviteCode);

    @Query("SELECT s FROM PlanningSession s LEFT JOIN FETCH s.participants WHERE s.id = :id")
    Optional<PlanningSession> findByIdWithParticipants(UUID id);
}
