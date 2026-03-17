package de.signaliduna.visualizer.fake.scenario;

import de.signaliduna.visualizer.fake.caller.HintRestCaller;
import de.signaliduna.visualizer.fake.caller.HintRestCaller.CallResult;
import de.signaliduna.visualizer.fake.kafka.HintKafkaProducer;
import de.signaliduna.visualizer.fake.kafka.HintKafkaProducer.BatchProduceResult;
import de.signaliduna.visualizer.fake.kafka.HintKafkaProducer.ProduceResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes test scenarios that exercise hint-service endpoints.
 * The visualizer UI calls these to trigger service-to-service communication.
 */
@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final HintRestCaller hintRestCaller;
    private final HintKafkaProducer hintKafkaProducer;

    public ScenarioController(HintRestCaller hintRestCaller, HintKafkaProducer hintKafkaProducer) {
        this.hintRestCaller = hintRestCaller;
        this.hintKafkaProducer = hintKafkaProducer;
    }

    // ==================== HTTP Scenarios ====================

    /**
     * Scenario: Get all hints (optionally filtered by processId).
     * Tests: GET /hints endpoint on hint-service.
     */
    @GetMapping("/http/get-hints")
    public CallResult scenarioGetHints(
            @RequestParam(required = false) String processId,
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        return hintRestCaller.getHints(processId, token);
    }

    /**
     * Scenario: Get a single hint by ID.
     * Tests: GET /hints/{id} endpoint on hint-service.
     */
    @GetMapping("/http/get-hint/{id}")
    public CallResult scenarioGetHintById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        return hintRestCaller.getHintById(id, token);
    }

    /**
     * Scenario: Save hints via REST.
     * Tests: POST /hints endpoint on hint-service.
     * Body should be a JSON array of HintDto objects.
     */
    @PostMapping("/http/save-hints")
    public CallResult scenarioSaveHints(
            @RequestBody String hintsJson,
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        return hintRestCaller.saveHints(hintsJson, token);
    }

    /**
     * Scenario: Search hints with multiple query parameters.
     * Tests: GET /hints?processId=X&hintSource=Y&hintCategory=Z on hint-service.
     */
    @GetMapping("/http/search-hints")
    public CallResult scenarioSearchHints(
            @RequestParam Map<String, String> queryParams,
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        return hintRestCaller.searchHints(queryParams, token);
    }

    // ==================== Kafka Scenarios ====================

    /**
     * Scenario: Send a single hint via Kafka.
     * Tests: hint-service's Spring Cloud Stream consumer (hintCreated function).
     */
    @PostMapping("/kafka/send-hint")
    public ProduceResult scenarioKafkaSendHint(@RequestBody KafkaSendRequest request) {
        return hintKafkaProducer.sendHint(
                request.hintSource(),
                request.message(),
                request.hintCategory(),
                request.showToUser(),
                request.processId()
        );
    }

    /**
     * Scenario: Send raw JSON payload to Kafka.
     * Tests: hint-service's deserialization and error handling.
     */
    @PostMapping("/kafka/send-raw")
    public ProduceResult scenarioKafkaSendRaw(@RequestBody RawKafkaRequest request) {
        return hintKafkaProducer.sendRawPayload(request.key(), request.payload());
    }

    /**
     * Scenario: Send a batch of hints via Kafka.
     * Tests: hint-service's Kafka consumer throughput.
     */
    @PostMapping("/kafka/send-batch")
    public BatchProduceResult scenarioKafkaSendBatch(@RequestBody BatchKafkaRequest request) {
        return hintKafkaProducer.sendHintBatch(
                request.count(),
                request.processIdPrefix(),
                request.hintCategory()
        );
    }

    // ==================== Full Flow Scenarios ====================

    /**
     * Scenario: Full round-trip test.
     * 1. Send hints via Kafka → hint-service consumes and saves.
     * 2. Wait briefly for processing.
     * 3. Retrieve hints via REST to verify they were saved.
     */
    @PostMapping("/full-flow/kafka-then-rest")
    public FullFlowResult scenarioFullFlow(
            @RequestBody FullFlowRequest request,
            @RequestHeader(value = "X-Target-Token", required = false) String token) {

        // Step 1: Send via Kafka
        var kafkaResult = hintKafkaProducer.sendHint(
                request.hintSource(),
                request.message(),
                request.hintCategory(),
                true,
                request.processId()
        );

        // Step 2: Wait for hint-service to consume
        try {
            Thread.sleep(request.waitMs() > 0 ? request.waitMs() : 2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Step 3: Retrieve via REST
        var restResult = hintRestCaller.getHints(request.processId(), token);

        return new FullFlowResult(kafkaResult, restResult);
    }

    // --- Request/Response records ---

    public record KafkaSendRequest(
            String hintSource,
            String message,
            String hintCategory,
            boolean showToUser,
            String processId
    ) {}

    public record RawKafkaRequest(String key, String payload) {}

    public record BatchKafkaRequest(int count, String processIdPrefix, String hintCategory) {}

    public record FullFlowRequest(
            String hintSource,
            String message,
            String hintCategory,
            String processId,
            long waitMs
    ) {}

    public record FullFlowResult(ProduceResult kafkaResult, CallResult restResult) {}
}
