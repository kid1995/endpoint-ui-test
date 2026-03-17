package de.signaliduna.visualizer.model;

import java.util.Map;

public record ProxyResponseDto(
        int statusCode,
        Map<String, String> headers,
        String body,
        long latencyMs,
        String traceId
) {
}
