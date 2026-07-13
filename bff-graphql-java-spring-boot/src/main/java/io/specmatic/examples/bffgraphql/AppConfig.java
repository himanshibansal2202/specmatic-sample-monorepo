package io.specmatic.examples.bffgraphql;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BackendProperties.class)
public class AppConfig {
  @Bean
  RestClient backendRestClient(BackendProperties properties) {
    CloseableHttpClient httpClient = HttpClients.custom().build();
    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .requestFactory(requestFactory)
        .build();
  }
}
