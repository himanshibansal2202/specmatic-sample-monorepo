package io.specmatic.sample.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backend")
public record BackendProperties(String baseUrl, String apiKey, String idempotencyKey) {
}
