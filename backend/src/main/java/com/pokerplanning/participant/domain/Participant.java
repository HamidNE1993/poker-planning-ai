package com.pokerplanning.participant.domain;

import com.pokerplanning.session.domain.PlanningSession;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "participants")
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
    private ParticipantRole role = ParticipantRole.VOTER;

    @Column(nullable = false)
    private boolean online = true;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @Column(nullable = false)
    private Instant lastHeartbeatAt = Instant.now();

    public Participant() {
    }

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

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PlanningSession getSession() {
        return session;
    }

    public void setSession(PlanningSession session) {
        this.session = session;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
