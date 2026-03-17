package de.signaliduna.visualizer.fake.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FakeCallerConfig {

    @Value("${hint-service.base-url:http://localhost:8082/api}")
    private String hintServiceBaseUrl;

    @Bean
    public RestClient hintServiceRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(hintServiceBaseUrl)
                .build();
    }
}
