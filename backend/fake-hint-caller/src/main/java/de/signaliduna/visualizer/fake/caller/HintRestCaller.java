package de.signaliduna.visualizer.fake.caller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Calls hint-service REST endpoints to test HTTP communication.
 * Mirrors the HintClient Feign interface from hint-client module.
 */
@Service
public class HintRestCaller {

    private static final Logger log = LoggerFactory.getLogger(HintRestCaller.class);

    private final RestClient hintServiceRestClient;
    private final ObjectMapper objectMapper;

    public HintRestCaller(RestClient hintServiceRestClient, ObjectMapper objectMapper) {
        this.hintServiceRestClient = hintServiceRestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /hints — retrieve all hints, optionally filtered by processId.
     */
    public CallResult getHints(String processId, String bearerToken) {
        var startTime = System.currentTimeMillis();
        try {
            var uri = processId != null ? "/hints?processId=" + processId : "/hints";
            var response = hintServiceRestClient.get()
                    .uri(uri)
                    .header("Authorization", bearerToken != null ? "Bearer " + bearerToken : "")
                    .retrieve()
                    .toEntity(String.class);

            return new CallResult(
                    "GET /hints",
                    response.getStatusCode().value(),
                    response.getBody(),
                    System.currentTimeMillis() - startTime,
                    true
            );
        } catch (RestClientResponseException ex) {
            return new CallResult("GET /hints", ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(), System.currentTimeMillis() - startTime, false);
        } catch (Exception ex) {
            log.error("GET /hints failed", ex);
            return new CallResult("GET /hints", 0, ex.getMessage(),
                    System.currentTimeMillis() - startTime, false);
        }
    }

    /**
     * GET /hints/{id} — retrieve a single hint by ID.
     */
    public CallResult getHintById(Long id, String bearerToken) {
        var startTime = System.currentTimeMillis();
        try {
            var response = hintServiceRestClient.get()
                    .uri("/hints/{id}", id)
                    .header("Authorization", bearerToken != null ? "Bearer " + bearerToken : "")
                    .retrieve()
                    .toEntity(String.class);

            return new CallResult(
                    "GET /hints/" + id,
                    response.getStatusCode().value(),
                    response.getBody(),
                    System.currentTimeMillis() - startTime,
                    true
            );
        } catch (RestClientResponseException ex) {
            return new CallResult("GET /hints/" + id, ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(), System.currentTimeMillis() - startTime, false);
        } catch (Exception ex) {
            log.error("GET /hints/{} failed", id, ex);
            return new CallResult("GET /hints/" + id, 0, ex.getMessage(),
                    System.currentTimeMillis() - startTime, false);
        }
    }

    /**
     * POST /hints — batch save hints.
     * Sends a list of HintDto JSON objects.
     */
    public CallResult saveHints(String hintsJson, String bearerToken) {
        var startTime = System.currentTimeMillis();
        try {
            var response = hintServiceRestClient.post()
                    .uri("/hints")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", bearerToken != null ? "Bearer " + bearerToken : "")
                    .body(hintsJson)
                    .retrieve()
                    .toEntity(String.class);

            return new CallResult(
                    "POST /hints",
                    response.getStatusCode().value(),
                    response.getBody(),
                    System.currentTimeMillis() - startTime,
                    true
            );
        } catch (RestClientResponseException ex) {
            return new CallResult("POST /hints", ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(), System.currentTimeMillis() - startTime, false);
        } catch (Exception ex) {
            log.error("POST /hints failed", ex);
            return new CallResult("POST /hints", 0, ex.getMessage(),
                    System.currentTimeMillis() - startTime, false);
        }
    }

    /**
     * GET /hints with complex query parameters — processId, hintSource, hintCategory, etc.
     */
    public CallResult searchHints(Map<String, String> queryParams, String bearerToken) {
        var startTime = System.currentTimeMillis();
        try {
            var queryString = queryParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .map(q -> "?" + q)
                    .orElse("");

            var response = hintServiceRestClient.get()
                    .uri("/hints" + queryString)
                    .header("Authorization", bearerToken != null ? "Bearer " + bearerToken : "")
                    .retrieve()
                    .toEntity(String.class);

            return new CallResult(
                    "GET /hints" + queryString,
                    response.getStatusCode().value(),
                    response.getBody(),
                    System.currentTimeMillis() - startTime,
                    true
            );
        } catch (RestClientResponseException ex) {
            return new CallResult("GET /hints (search)", ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(), System.currentTimeMillis() - startTime, false);
        } catch (Exception ex) {
            log.error("GET /hints (search) failed", ex);
            return new CallResult("GET /hints (search)", 0, ex.getMessage(),
                    System.currentTimeMillis() - startTime, false);
        }
    }

    public record CallResult(
            String endpoint,
            int statusCode,
            String body,
            long latencyMs,
            boolean success
    ) {}
}
