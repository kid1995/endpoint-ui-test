package de.signaliduna.visualizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceEdgeDto(
        Long id,
        @NotNull Long sourceNodeId,
        @NotNull Long targetNodeId,
        @NotNull EdgeType edgeType,
        String label,
        Long latencyMs
) {
    public enum EdgeType {
        HTTP, KAFKA
    }
}
