package de.signaliduna.visualizer.model;

import jakarta.validation.constraints.NotEmpty;

public record KafkaMessageDto(
        @NotEmpty String topic,
        String key,
        @NotEmpty String payload,
        String sessionId,
        String sourceNodeName
) {
}
