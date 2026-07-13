package io.specmatic.examples.bffgraphql;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backend")
public record BackendProperties(String baseUrl, String authenticateHeader, String idempotencyKey) {
}
