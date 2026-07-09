package io.specmatic.samples.bff;

import com.fasterxml.jackson.databind.MapperFeature;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
class AppConfig {
  @Bean
  RestTemplate restTemplate(RestTemplateBuilder builder, @Value("${backend.base-url}") String backendBaseUrl) {
    return builder
        .rootUri(backendBaseUrl)
        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()))
        .build();
  }

  @Bean
  Jackson2ObjectMapperBuilderCustomizer strictJsonTypes() {
    return builder -> builder.featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
  }
}
