package de.signaliduna.visualizer.fake.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Produces HintDto messages to the Kafka topic that hint-service consumes.
 * Simulates upstream services that create hints via async messaging.
 */
@Service
public class HintKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(HintKafkaProducer.class);
    private static final long SEND_TIMEOUT_SEC = 10L;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${hint-service.kafka.topic:elpa-hint-created}")
    private String hintTopic;

    public HintKafkaProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send a single HintDto as JSON to the hint-created topic.
     */
    public ProduceResult sendHint(String hintSource, String message, String category,
                                   boolean showToUser, String processId) {
        var startTime = System.currentTimeMillis();
        try {
            var hintPayload = Map.of(
                    "hintSource", hintSource,
                    "message", message,
                    "hintCategory", category,
                    "showToUser", showToUser,
                    "processId", processId,
                    "creationDate", LocalDateTime.now().toString()
            );
            var json = objectMapper.writeValueAsString(hintPayload);

            var future = kafkaTemplate.send(hintTopic, processId, json);
            future.get(SEND_TIMEOUT_SEC, TimeUnit.SECONDS);

            var latencyMs = System.currentTimeMillis() - startTime;
            log.info("Hint message sent to topic {} in {}ms: processId={}", hintTopic, latencyMs, processId);

            return new ProduceResult(hintTopic, json, latencyMs, true, null);
        } catch (Exception ex) {
            var latencyMs = System.currentTimeMillis() - startTime;
            log.error("Failed to send hint to Kafka topic {}", hintTopic, ex);
            return new ProduceResult(hintTopic, null, latencyMs, false, ex.getMessage());
        }
    }

    /**
     * Send raw JSON payload to the hint topic (for custom payloads).
     */
    public ProduceResult sendRawPayload(String key, String jsonPayload) {
        var startTime = System.currentTimeMillis();
        try {
            var future = kafkaTemplate.send(hintTopic, key, jsonPayload);
            future.get(SEND_TIMEOUT_SEC, TimeUnit.SECONDS);

            var latencyMs = System.currentTimeMillis() - startTime;
            log.info("Raw payload sent to topic {} in {}ms", hintTopic, latencyMs);
            return new ProduceResult(hintTopic, jsonPayload, latencyMs, true, null);
        } catch (Exception ex) {
            var latencyMs = System.currentTimeMillis() - startTime;
            log.error("Failed to send raw payload to Kafka topic {}", hintTopic, ex);
            return new ProduceResult(hintTopic, jsonPayload, latencyMs, false, ex.getMessage());
        }
    }

    /**
     * Send a batch of hints to test hint-service Kafka consumer throughput.
     */
    public BatchProduceResult sendHintBatch(int count, String processIdPrefix, String category) {
        var startTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < count; i++) {
            var result = sendHint(
                    "FAKE-SERVICE",
                    "Test hint message #" + (i + 1),
                    category,
                    true,
                    processIdPrefix + "-" + i
            );
            if (result.success()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        var totalLatencyMs = System.currentTimeMillis() - startTime;
        return new BatchProduceResult(count, successCount, failCount, totalLatencyMs);
    }

    public record ProduceResult(String topic, String payload, long latencyMs, boolean success, String error) {}

    public record BatchProduceResult(int total, int success, int failed, long totalLatencyMs) {}
}
