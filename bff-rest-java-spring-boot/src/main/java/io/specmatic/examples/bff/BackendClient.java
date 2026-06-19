package io.specmatic.examples.bff;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class BackendClient {
  private final RestClient restClient;
  private final String apiKey;

  BackendClient(@Value("${backend.base-url}") String backendBaseUrl,
      @Value("${backend.api-key}") String apiKey) {
    this.restClient = RestClient.builder().baseUrl(backendBaseUrl).build();
    this.apiKey = apiKey;
  }

  Id createProduct(ProductBase product) {
    return restClient.post()
        .uri("/products")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authenticate", apiKey)
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .body(java.util.Map.of(
            "name", product.name(),
            "type", product.type(),
            "inventory", product.inventory()))
        .retrieve()
        .body(Id.class);
  }

  List<Product> findAvailableProducts(ProductType type, Integer pageSize, LocalDate fromDate, LocalDate toDate) {
    URI uri = UriComponentsBuilder.fromPath("/products")
        .queryParamIfPresent("type", java.util.Optional.ofNullable(type).map(ProductType::name))
        .queryParamIfPresent("from-date", java.util.Optional.ofNullable(fromDate).map(LocalDate::toString))
        .queryParamIfPresent("to-date", java.util.Optional.ofNullable(toDate).map(LocalDate::toString))
        .build()
        .toUri();

    RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri);
    if (pageSize != null) {
      request = request.header("pageSize", String.valueOf(pageSize));
    }

    return request.retrieve().body(new ParameterizedTypeReference<>() {});
  }

  Id createOrder(OrderBase order) {
    return restClient.post()
        .uri("/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authenticate", apiKey)
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .body(order)
        .retrieve()
        .body(Id.class);
  }

  List<Order> getOrders() {
    return restClient.get()
        .uri("/orders")
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  Order getOrder(Integer orderId) {
    return restClient.get()
        .uri("/orders/{id}", orderId)
        .retrieve()
        .body(Order.class);
  }
}
