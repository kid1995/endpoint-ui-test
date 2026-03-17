package de.signaliduna.visualizer.fake.scenario;

import de.signaliduna.visualizer.fake.caller.HintRestCaller;
import de.signaliduna.visualizer.fake.caller.HintRestCaller.CallResult;
import de.signaliduna.visualizer.fake.kafka.HintKafkaProducer;
import de.signaliduna.visualizer.fake.kafka.HintKafkaProducer.ProduceResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pre-built test scenarios with sensible defaults.
 * One-click tests that exercise all hint-service endpoints.
 */
@RestController
@RequestMapping("/api/scenarios/preloaded")
public class PreloadedScenarioController {

    private final HintRestCaller hintRestCaller;
    private final HintKafkaProducer hintKafkaProducer;

    public PreloadedScenarioController(HintRestCaller hintRestCaller, HintKafkaProducer hintKafkaProducer) {
        this.hintRestCaller = hintRestCaller;
        this.hintKafkaProducer = hintKafkaProducer;
    }

    /**
     * Test all 4 hint categories via REST POST.
     */
    @PostMapping("/rest-all-categories")
    public List<CallResult> testAllCategoriesViaRest(
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        var processId = "test-" + UUID.randomUUID();
        return List.of(
                hintRestCaller.saveHints(HintTestDataFactory.infoHintJson(processId), token),
                hintRestCaller.saveHints(HintTestDataFactory.warningHintJson(processId), token),
                hintRestCaller.saveHints(HintTestDataFactory.errorHintJson(processId), token),
                hintRestCaller.saveHints(HintTestDataFactory.blockerHintJson(processId), token)
        );
    }

    /**
     * Test all 4 hint categories via Kafka.
     */
    @PostMapping("/kafka-all-categories")
    public List<ProduceResult> testAllCategoriesViaKafka() {
        var processId = "kafka-test-" + UUID.randomUUID();
        return List.of(
                hintKafkaProducer.sendHint("FAKE-SERVICE", "Info via Kafka", "INFO", true, processId + "-info"),
                hintKafkaProducer.sendHint("FAKE-SERVICE", "Warning via Kafka", "WARNING", true, processId + "-warn"),
                hintKafkaProducer.sendHint("FAKE-SERVICE", "Error via Kafka", "ERROR", true, processId + "-error"),
                hintKafkaProducer.sendHint("FAKE-SERVICE", "Blocker via Kafka", "BLOCKER", true, processId + "-block")
        );
    }

    /**
     * Test hint-service error handling with invalid data.
     */
    @PostMapping("/error-handling")
    public Map<String, CallResult> testErrorHandling(
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        return Map.of(
                "empty_fields", hintRestCaller.saveHints(HintTestDataFactory.invalidHintJson(), token),
                "malformed_json", hintRestCaller.saveHints(HintTestDataFactory.malformedJson(), token),
                "no_auth", hintRestCaller.getHints(null, null)
        );
    }

    /**
     * Test search/query parameters.
     */
    @GetMapping("/search-combinations")
    public Map<String, CallResult> testSearchCombinations(
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        return Map.of(
                "by_process_id", hintRestCaller.searchHints(Map.of("processId", "test-process"), token),
                "by_source_prefix", hintRestCaller.searchHints(Map.of("processId", "test", "hintSourcePrefix", "FAKE"), token),
                "by_category", hintRestCaller.searchHints(Map.of("processId", "test", "hintCategory", "ERROR"), token)
        );
    }

    /**
     * Test Kafka consumer error handling with bad payloads.
     */
    @PostMapping("/kafka-error-handling")
    public Map<String, ProduceResult> testKafkaErrorHandling() {
        return Map.of(
                "malformed_json", hintKafkaProducer.sendRawPayload("bad-key", HintTestDataFactory.malformedJson()),
                "invalid_fields", hintKafkaProducer.sendRawPayload("bad-fields", HintTestDataFactory.invalidHintJson())
        );
    }

    /**
     * Full throughput test: sends N hints via Kafka, then queries via REST.
     */
    @PostMapping("/throughput-test")
    public ThroughputResult testThroughput(
            @RequestParam(defaultValue = "10") int count,
            @RequestHeader(value = "X-Target-Token", required = false) String token) {
        var prefix = "throughput-" + UUID.randomUUID();

        var batchResult = hintKafkaProducer.sendHintBatch(count, prefix, "INFO");

        // Wait for processing
        try {
            Thread.sleep(Math.min(count * 200L, 5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var restResult = hintRestCaller.searchHints(Map.of("processId", prefix + "-0"), token);
        return new ThroughputResult(batchResult, restResult);
    }

    public record ThroughputResult(
            HintKafkaProducer.BatchProduceResult kafkaBatch,
            CallResult verificationQuery
    ) {}
}
