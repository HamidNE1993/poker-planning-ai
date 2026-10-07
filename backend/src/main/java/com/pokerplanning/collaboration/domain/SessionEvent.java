package com.pokerplanning.collaboration.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionEvent<T>(
    UUID sessionId,
    SessionEventType type,
    T payload,
    Instant timestamp
) {
    public static <T> SessionEvent<T> of(UUID sessionId, SessionEventType type, T payload) {
        return new SessionEvent<>(sessionId, type, payload, Instant.now());
    }

    public static SessionEvent<Void> of(UUID sessionId, SessionEventType type) {
        return new SessionEvent<>(sessionId, type, null, Instant.now());
    }
}
