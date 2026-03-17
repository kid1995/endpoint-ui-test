package de.signaliduna.visualizer.core;

import de.signaliduna.visualizer.model.KafkaMessageDto;
import de.signaliduna.visualizer.model.TransactionEventDto;
import de.signaliduna.visualizer.model.TransactionEventDto.EventStatus;
import de.signaliduna.visualizer.model.TransactionEventDto.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaProxyService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProxyService.class);
    private static final long SEND_TIMEOUT_SEC = 10L;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TelemetryService telemetryService;

    public KafkaProxyService(KafkaTemplate<String, String> kafkaTemplate,
                             TelemetryService telemetryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.telemetryService = telemetryService;
    }

    public void produce(KafkaMessageDto message) {
        var sessionId = message.sessionId() != null ? message.sessionId() : "default";
        var traceId = UUID.randomUUID().toString();
        var startTime = System.currentTimeMillis();

        publishProduceEvent(sessionId, message, traceId, EventStatus.IN_FLIGHT);

        try {
            var future = kafkaTemplate.send(message.topic(), message.key(), message.payload());
            future.get(SEND_TIMEOUT_SEC, TimeUnit.SECONDS);
            var latencyMs = System.currentTimeMillis() - startTime;

            log.info("Kafka message sent to topic: {} in {}ms", message.topic(), latencyMs);
            publishProduceEvent(sessionId, message, traceId, EventStatus.SUCCESS);
        } catch (Exception ex) {
            log.error("Failed to send Kafka message to topic: {}", message.topic(), ex);
            publishProduceEvent(sessionId, message, traceId, EventStatus.FAILURE);
        }
    }

    private void publishProduceEvent(String sessionId, KafkaMessageDto message,
                                     String traceId, EventStatus status) {
        var event = new TransactionEventDto(
                UUID.randomUUID().toString(), sessionId,
                message.sourceNodeName(), "kafka:" + message.topic(),
                EventType.KAFKA_PRODUCE, status,
                null, Instant.now(), traceId,
                message.payload(), null, null
        );
        telemetryService.publishEvent(sessionId, event);
    }
}
