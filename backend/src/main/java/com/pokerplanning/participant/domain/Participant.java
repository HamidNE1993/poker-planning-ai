package com.pokerplanning.participant.domain;

import com.pokerplanning.session.domain.PlanningSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PlanningSession session;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.VOTER;

    @Column(nullable = false)
    @Builder.Default
    private boolean online = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant joinedAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant lastHeartbeatAt = Instant.now();

    public Participant(PlanningSession session, String name, String avatar, ParticipantRole role) {
        this.session = session;
        this.name = name;
        this.avatar = avatar;
        this.role = role != null ? role : ParticipantRole.VOTER;
        this.online = true;
        this.joinedAt = Instant.now();
        this.lastHeartbeatAt = Instant.now();
    }

    public void heartbeat() {
        this.lastHeartbeatAt = Instant.now();
        this.online = true;
    }
}
