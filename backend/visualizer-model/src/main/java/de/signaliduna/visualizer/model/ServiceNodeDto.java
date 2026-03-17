package de.signaliduna.visualizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceNodeDto(
        Long id,
        @NotEmpty String name,
        @NotEmpty String baseUrl,
        String kafkaTopic,
        String mockResponse,
        @NotNull ServiceStatus status,
        @NotNull Double positionX,
        @NotNull Double positionY,
        Long appId
) {
    public enum ServiceStatus {
        ONLINE, OFFLINE, DEGRADED, UNKNOWN
    }
}
