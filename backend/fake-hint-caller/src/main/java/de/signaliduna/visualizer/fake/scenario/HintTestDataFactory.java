package de.signaliduna.visualizer.fake.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Factory for generating test HintDto payloads matching hint-service's expected format.
 * Based on the HintDto record from hint-model module.
 */
public final class HintTestDataFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private HintTestDataFactory() {}

    public static String singleHintJson(String hintSource, String message,
                                         String category, String processId) {
        var hint = Map.of(
                "hintSource", hintSource,
                "message", message,
                "hintCategory", category,
                "showToUser", true,
                "processId", processId,
                "creationDate", LocalDateTime.now().toString()
        );
        return toJson(List.of(hint));
    }

    public static String batchHintsJson(int count, String processIdPrefix, String category) {
        var hints = IntStream.range(0, count)
                .mapToObj(i -> Map.<String, Object>of(
                        "hintSource", "FAKE-SERVICE",
                        "message", "Batch test hint #" + (i + 1),
                        "hintCategory", category,
                        "showToUser", true,
                        "processId", processIdPrefix + "-" + i,
                        "creationDate", LocalDateTime.now().toString()
                ))
                .toList();
        return toJson(hints);
    }

    public static String infoHintJson(String processId) {
        return singleHintJson("FAKE-SERVICE", "Informational test hint", "INFO", processId);
    }

    public static String warningHintJson(String processId) {
        return singleHintJson("FAKE-SERVICE", "Warning test hint", "WARNING", processId);
    }

    public static String errorHintJson(String processId) {
        return singleHintJson("FAKE-SERVICE", "Error test hint", "ERROR", processId);
    }

    public static String blockerHintJson(String processId) {
        return singleHintJson("FAKE-SERVICE", "Blocker test hint", "BLOCKER", processId);
    }

    /**
     * Generates an invalid payload to test hint-service error handling.
     */
    public static String invalidHintJson() {
        return "[{\"hintSource\": \"\", \"message\": \"\"}]";
    }

    /**
     * Generates a malformed JSON payload to test deserialization error handling.
     */
    public static String malformedJson() {
        return "{this is not valid json";
    }

    private static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize test data", e);
        }
    }
}
