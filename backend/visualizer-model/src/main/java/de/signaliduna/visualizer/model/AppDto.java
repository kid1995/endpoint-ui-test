package de.signaliduna.visualizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppDto(
        Long id,
        @NotEmpty String name,
        String description,
        List<Long> nodeIds
) {
}
