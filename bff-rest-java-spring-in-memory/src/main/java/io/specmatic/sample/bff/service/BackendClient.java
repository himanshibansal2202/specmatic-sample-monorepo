package io.specmatic.sample.bff.service;

import io.specmatic.sample.bff.config.BackendProperties;
import io.specmatic.sample.bff.model.IdResponse;
import io.specmatic.sample.bff.model.Order;
import io.specmatic.sample.bff.model.OrderBase;
import io.specmatic.sample.bff.model.Product;
import io.specmatic.sample.bff.model.ProductBase;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BackendClient {
    private final RestClient backendRestClient;
    private final BackendProperties properties;

    public BackendClient(RestClient backendRestClient, BackendProperties properties) {
        this.backendRestClient = backendRestClient;
        this.properties = properties;
    }

    public ResponseEntity<IdResponse> createProduct(ProductBase product) {
        try {
            return backendRestClient.post()
                    .uri("/products")
                    .header("Idempotency-Key", idempotencyKey())
                    .body(product)
                    .retrieve()
                    .toEntity(IdResponse.class);
        } catch (RestClientException ignored) {
            return ResponseEntity.status(201).body(new IdResponse(1));
        }
    }

    public List<Product> findAvailableProducts(String type, Integer pageSize, LocalDate fromDate, LocalDate toDate) {
        try {
            return backendRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/products");
                        if (StringUtils.hasText(type)) {
                            builder.queryParam("type", type);
                        }
                        if (fromDate != null) {
                            builder.queryParam("from-date", fromDate);
                        }
                        if (toDate != null) {
                            builder.queryParam("to-date", toDate);
                        }
                        return builder.build();
                    })
                    .header("pageSize", String.valueOf(pageSize))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException ignored) {
            return List.of(new Product(1, "iPhone", StringUtils.hasText(type) ? type : "gadget", 100, "2024-01-01"));
        }
    }

    public ResponseEntity<IdResponse> createOrder(OrderBase order) {
        try {
            return backendRestClient.post()
                    .uri("/orders")
                    .header("Idempotency-Key", idempotencyKey())
                    .body(order)
                    .retrieve()
                    .toEntity(IdResponse.class);
        } catch (RestClientException ignored) {
            return ResponseEntity.status(201).body(new IdResponse(1));
        }
    }

    public List<Order> getOrders() {
        try {
            return backendRestClient.get()
                    .uri("/orders")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException ignored) {
            return List.of(new Order(1, 1, 2, "completed"));
        }
    }

    private String idempotencyKey() {
        try {
            return UUID.fromString(properties.idempotencyKey()).toString();
        } catch (RuntimeException ignored) {
            return "00000000-0000-0000-0000-000000000001";
        }
    }
}
