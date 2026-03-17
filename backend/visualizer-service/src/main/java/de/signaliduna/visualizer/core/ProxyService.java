package de.signaliduna.visualizer.core;

import de.signaliduna.visualizer.model.ProxyRequestDto;
import de.signaliduna.visualizer.model.ProxyResponseDto;
import de.signaliduna.visualizer.model.TransactionEventDto;
import de.signaliduna.visualizer.model.TransactionEventDto.EventStatus;
import de.signaliduna.visualizer.model.TransactionEventDto.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    private final RestClient restClient;
    private final TelemetryService telemetryService;

    public ProxyService(RestClient.Builder restClientBuilder, TelemetryService telemetryService) {
        this.restClient = restClientBuilder.build();
        this.telemetryService = telemetryService;
    }

    public ProxyResponseDto forward(ProxyRequestDto request, String bearerToken) {
        var traceId = UUID.randomUUID().toString();
        var sessionId = request.sessionId() != null ? request.sessionId() : "default";

        publishInFlightEvent(sessionId, request, traceId);

        var startTime = System.currentTimeMillis();
        try {
            var spec = buildRequest(request, bearerToken, traceId);
            var response = spec.retrieve().toEntity(String.class);
            var latencyMs = System.currentTimeMillis() - startTime;

            var headers = new HashMap<String, String>();
            response.getHeaders().forEach((key, values) -> headers.put(key, String.join(", ", values)));

            var result = new ProxyResponseDto(
                    response.getStatusCode().value(),
                    Map.copyOf(headers),
                    response.getBody(),
                    latencyMs,
                    traceId
            );

            publishResponseEvent(sessionId, request, traceId, latencyMs, EventStatus.SUCCESS,
                    response.getStatusCode().value(), response.getBody());

            return result;
        } catch (RestClientResponseException ex) {
            var latencyMs = System.currentTimeMillis() - startTime;
            publishResponseEvent(sessionId, request, traceId, latencyMs, EventStatus.FAILURE,
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());

            return new ProxyResponseDto(
                    ex.getStatusCode().value(),
                    Map.of(),
                    ex.getResponseBodyAsString(),
                    latencyMs,
                    traceId
            );
        } catch (Exception ex) {
            var latencyMs = System.currentTimeMillis() - startTime;
            log.error("Proxy request failed: {} {}", request.method(), request.targetUrl(), ex);

            publishResponseEvent(sessionId, request, traceId, latencyMs, EventStatus.FAILURE,
                    0, ex.getMessage());

            return new ProxyResponseDto(0, Map.of(), ex.getMessage(), latencyMs, traceId);
        }
    }

    private RestClient.RequestBodySpec buildRequest(ProxyRequestDto request, String bearerToken, String traceId) {
        var spec = restClient.method(org.springframework.http.HttpMethod.valueOf(request.method().name()))
                .uri(request.targetUrl())
                .header("X-Trace-Id", traceId)
                .contentType(MediaType.APPLICATION_JSON);

        if (bearerToken != null && !bearerToken.isBlank()) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        if (request.headers() != null) {
            request.headers().forEach(spec::header);
        }

        if (request.body() != null) {
            spec.body(request.body());
        }

        return spec;
    }

    private void publishInFlightEvent(String sessionId, ProxyRequestDto request, String traceId) {
        var event = new TransactionEventDto(
                UUID.randomUUID().toString(), sessionId,
                request.sourceNodeName(), request.targetNodeName(),
                EventType.HTTP_REQUEST, EventStatus.IN_FLIGHT,
                null, Instant.now(), traceId,
                request.body(), null, null
        );
        telemetryService.publishEvent(sessionId, event);
    }

    private void publishResponseEvent(String sessionId, ProxyRequestDto request, String traceId,
                                      long latencyMs, EventStatus status, int httpStatus, String responseBody) {
        var event = new TransactionEventDto(
                UUID.randomUUID().toString(), sessionId,
                request.targetNodeName(), request.sourceNodeName(),
                EventType.HTTP_RESPONSE, status,
                latencyMs, Instant.now(), traceId,
                null, responseBody, httpStatus
        );
        telemetryService.publishEvent(sessionId, event);
    }
}
