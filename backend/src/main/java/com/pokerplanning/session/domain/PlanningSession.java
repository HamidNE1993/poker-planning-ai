package com.pokerplanning.session.domain;

import com.pokerplanning.participant.domain.Participant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "planning_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PlanningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String sprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SessionStatus status = SessionStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DeckType deckType = DeckType.FIBONACCI;

    @Column(nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Column(nullable = false)
    @Builder.Default
    private boolean autoReveal = false;

    @Column
    @Builder.Default
    private Integer timerDurationSeconds = 60;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Participant> participants = new ArrayList<>();

    public PlanningSession(String name, String sprint, DeckType deckType, String inviteCode) {
        this.name = name;
        this.sprint = sprint;
        this.deckType = deckType != null ? deckType : DeckType.FIBONACCI;
        this.inviteCode = inviteCode;
        this.status = SessionStatus.CREATED;
        this.participants = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isCompleted() {
        return this.status == SessionStatus.COMPLETED;
    }

    public void updateConfiguration(DeckType deckType, Boolean autoReveal, Integer timerDurationSeconds) {
        if (isCompleted()) {
            throw new IllegalStateException("Impossible de modifier la configuration d'une session terminée.");
        }
        if (deckType != null) {
            this.deckType = deckType;
        }
        if (autoReveal != null) {
            this.autoReveal = autoReveal;
        }
        if (timerDurationSeconds != null) {
            this.timerDurationSeconds = timerDurationSeconds;
        }
    }

    public void changeStatus(SessionStatus newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    public void addParticipant(Participant participant) {
        if (this.participants == null) {
            this.participants = new ArrayList<>();
        }
        participants.add(participant);
        participant.setSession(this);
    }

    public void removeParticipant(Participant participant) {
        if (this.participants != null) {
            participants.remove(participant);
        }
        participant.setSession(null);
    }
}
