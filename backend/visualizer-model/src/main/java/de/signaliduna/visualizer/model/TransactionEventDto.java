package de.signaliduna.visualizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionEventDto(
        String id,
        String sessionId,
        String sourceNode,
        String targetNode,
        EventType eventType,
        EventStatus status,
        Long latencyMs,
        Instant timestamp,
        String traceId,
        String payload,
        String responseBody,
        Integer httpStatus
) {
    public enum EventType {
        HTTP_REQUEST, HTTP_RESPONSE, KAFKA_PRODUCE, KAFKA_CONSUME
    }

    public enum EventStatus {
        IN_FLIGHT, SUCCESS, FAILURE, TIMEOUT
    }
}
