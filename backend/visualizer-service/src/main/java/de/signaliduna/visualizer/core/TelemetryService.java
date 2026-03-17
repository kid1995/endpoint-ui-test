package de.signaliduna.visualizer.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.signaliduna.visualizer.model.TransactionEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final long HEARTBEAT_INTERVAL_SEC = 15L;

    private final Map<String, List<SseEmitter>> sessionEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper objectMapper;

    public TelemetryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        startHeartbeat();
    }

    public SseEmitter subscribe(String sessionId) {
        var emitter = new SseEmitter(SSE_TIMEOUT_MS);
        var emitters = sessionEmitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(e -> removeEmitter(sessionId, emitter));

        log.info("SSE subscriber connected for session: {}", sessionId);
        return emitter;
    }

    public void publishEvent(String sessionId, TransactionEventDto event) {
        var emitters = sessionEmitters.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No subscribers for session: {}", sessionId);
            return;
        }

        try {
            var json = objectMapper.writeValueAsString(event);
            var sseEvent = SseEmitter.event()
                    .name(event.eventType().name())
                    .data(json);

            for (var emitter : emitters) {
                try {
                    emitter.send(sseEvent);
                } catch (IOException e) {
                    removeEmitter(sessionId, emitter);
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize event for session: {}", sessionId, e);
        }
    }

    public void publishToAll(TransactionEventDto event) {
        sessionEmitters.keySet().forEach(sessionId -> publishEvent(sessionId, event));
    }

    public void closeSession(String sessionId) {
        var emitters = sessionEmitters.remove(sessionId);
        if (emitters != null) {
            emitters.forEach(SseEmitter::complete);
        }
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        var emitters = sessionEmitters.get(sessionId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                sessionEmitters.remove(sessionId);
            }
        }
    }

    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            sessionEmitters.forEach((sessionId, emitters) -> {
                for (var emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event().comment("ping"));
                    } catch (IOException e) {
                        removeEmitter(sessionId, emitter);
                    }
                }
            });
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }
}
