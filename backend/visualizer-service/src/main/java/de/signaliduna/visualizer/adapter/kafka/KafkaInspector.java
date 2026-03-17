package de.signaliduna.visualizer.adapter.kafka;

import de.signaliduna.visualizer.core.TelemetryService;
import de.signaliduna.visualizer.model.TransactionEventDto;
import de.signaliduna.visualizer.model.TransactionEventDto.EventStatus;
import de.signaliduna.visualizer.model.TransactionEventDto.EventType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only Kafka consumer that monitors configured topics
 * and publishes events to the SSE telemetry stream.
 */
@Component
public class KafkaInspector {

    private static final Logger log = LoggerFactory.getLogger(KafkaInspector.class);

    private final TelemetryService telemetryService;

    public KafkaInspector(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @KafkaListener(
            topics = "${visualizer.kafka.inspect-topics:elpa-hint-created}",
            groupId = "${visualizer.kafka.inspector-group:visualizer-inspector}",
            autoStartup = "${visualizer.kafka.inspector-enabled:true}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Kafka message intercepted on topic: {} partition: {} offset: {}",
                record.topic(), record.partition(), record.offset());

        var event = new TransactionEventDto(
                UUID.randomUUID().toString(),
                "default",
                "kafka:" + record.topic(),
                "consumer:" + record.topic(),
                EventType.KAFKA_CONSUME,
                EventStatus.SUCCESS,
                null,
                Instant.ofEpochMilli(record.timestamp()),
                record.key(),
                record.value(),
                null,
                null
        );

        telemetryService.publishToAll(event);
    }
}
