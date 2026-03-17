package de.signaliduna.visualizer.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ProxyRequestDto(
        @NotEmpty String targetUrl,
        @NotNull HttpMethod method,
        Map<String, String> headers,
        String body,
        String sessionId,
        String sourceNodeName,
        String targetNodeName
) {
    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH
    }
}
