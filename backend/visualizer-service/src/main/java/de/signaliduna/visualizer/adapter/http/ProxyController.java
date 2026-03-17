package de.signaliduna.visualizer.adapter.http;

import de.signaliduna.visualizer.core.KafkaProxyService;
import de.signaliduna.visualizer.core.ProxyService;
import de.signaliduna.visualizer.model.KafkaMessageDto;
import de.signaliduna.visualizer.model.ProxyRequestDto;
import de.signaliduna.visualizer.model.ProxyResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    private final ProxyService proxyService;
    private final KafkaProxyService kafkaProxyService;

    public ProxyController(ProxyService proxyService, KafkaProxyService kafkaProxyService) {
        this.proxyService = proxyService;
        this.kafkaProxyService = kafkaProxyService;
    }

    @PostMapping("/http")
    public ProxyResponseDto forwardHttp(
            @Valid @RequestBody ProxyRequestDto request,
            @RequestHeader(value = "X-Target-Token", required = false) String targetToken) {
        return proxyService.forward(request, targetToken);
    }

    @PostMapping("/kafka")
    public ResponseEntity<Void> produceKafka(@Valid @RequestBody KafkaMessageDto message) {
        kafkaProxyService.produce(message);
        return ResponseEntity.accepted().build();
    }
}
