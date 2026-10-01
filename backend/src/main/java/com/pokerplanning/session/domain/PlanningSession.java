package com.pokerplanning.session.domain;

import com.pokerplanning.participant.domain.Participant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "planning_sessions")
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
    private SessionStatus status = SessionStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeckType deckType = DeckType.FIBONACCI;

    @Column(nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Column(nullable = false)
    private boolean autoReveal = false;

    @Column
    private Integer timerDurationSeconds = 60;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    public PlanningSession() {
    }

    public PlanningSession(String name, String sprint, DeckType deckType, String inviteCode) {
        this.name = name;
        this.sprint = sprint;
        this.deckType = deckType != null ? deckType : DeckType.FIBONACCI;
        this.inviteCode = inviteCode;
        this.status = SessionStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
        participant.setSession(this);
    }

    public void removeParticipant(Participant participant) {
        participants.remove(participant);
        participant.setSession(null);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSprint() {
        return sprint;
    }

    public void setSprint(String sprint) {
        this.sprint = sprint;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public DeckType getDeckType() {
        return deckType;
    }

    public void setDeckType(DeckType deckType) {
        this.deckType = deckType;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public boolean isAutoReveal() {
        return autoReveal;
    }

    public void setAutoReveal(boolean autoReveal) {
        this.autoReveal = autoReveal;
    }

    public Integer getTimerDurationSeconds() {
        return timerDurationSeconds;
    }

    public void setTimerDurationSeconds(Integer timerDurationSeconds) {
        this.timerDurationSeconds = timerDurationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanningSession that = (PlanningSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
