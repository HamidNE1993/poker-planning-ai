package com.pokerplanning.collaboration.application;

import com.pokerplanning.collaboration.domain.SessionEvent;
import com.pokerplanning.collaboration.domain.SessionEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SessionEventPublisher {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private final Map<UUID, List<SseEmitter>> sessionEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID sessionId) {
        log.info("Nouveau client SSE connecté à la session {}", sessionId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        sessionEmitters.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> emitters = sessionEmitters.get(sessionId);
            if (emitters != null) {
                emitters.remove(emitter);
                if (emitters.isEmpty()) {
                    sessionEmitters.remove(sessionId);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            log.debug("Timeout SSE pour la session {}", sessionId);
            emitter.complete();
            cleanup.run();
        });
        emitter.onError(e -> {
            log.debug("Erreur SSE pour la session {}: {}", sessionId, e.getMessage());
            cleanup.run();
        });

        // Envoi d'un événement initial pour initialiser le flux
        try {
            SessionEvent<String> initEvent = SessionEvent.of(sessionId, SessionEventType.HEARTBEAT, "connected");
            emitter.send(SseEmitter.event().name(SessionEventType.HEARTBEAT.name()).data(initEvent));
        } catch (IOException e) {
            log.warn("Impossible d'envoyer l'événement initial au client SSE pour la session {}", sessionId);
            cleanup.run();
        }

        return emitter;
    }

    public <T> void publish(UUID sessionId, SessionEventType type, T payload) {
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("Aucun client SSE abonné à la session {}", sessionId);
            return;
        }

        SessionEvent<T> event = SessionEvent.of(sessionId, type, payload);
        log.debug("Diffusion de l'événement {} pour la session {} à {} client(s)", type, sessionId, emitters.size());

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(type.name()).data(event));
            } catch (Exception e) {
                log.debug("Erreur d'envoi SSE, suppression du client pour la session {}: {}", sessionId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                sessionEmitters.remove(sessionId);
            }
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (sessionEmitters.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, List<SseEmitter>> entry : sessionEmitters.entrySet()) {
            UUID sessionId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            SessionEvent<String> ping = SessionEvent.of(sessionId, SessionEventType.HEARTBEAT, "ping");

            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(SessionEventType.HEARTBEAT.name()).data(ping));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }

            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
            }
        }
    }
}
