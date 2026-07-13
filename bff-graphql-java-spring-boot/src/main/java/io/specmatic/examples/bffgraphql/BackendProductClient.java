package io.specmatic.examples.bffgraphql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BackendProductClient {
  private final RestClient restClient;
  private final BackendProperties properties;

  public BackendProductClient(RestClient restClient, BackendProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public List<Product> findAvailableProducts(ProductType type, Integer pageSize) {
    var request = restClient.get()
        .uri(uriBuilder -> uriBuilder.path("/products").queryParam("type", type.name()).build());
    if (pageSize != null) {
      request.header("pageSize", pageSize.toString());
    }

    ProductResponse[] products = request.retrieve().body(ProductResponse[].class);

    if (products == null) {
      return List.of();
    }

    return Arrays.stream(products)
        .map(ProductResponse::toGraphqlProduct)
        .toList();
  }

  public Product createProduct(NewProductInput input) {
    IdResponse response = restClient.post()
        .uri("/products")
        .header("Authenticate", properties.authenticateHeader())
        .header("Idempotency-Key", properties.idempotencyKey())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of(
            "name", input.name(),
            "inventory", input.inventory(),
            "type", input.type().name()))
        .retrieve()
        .body(new ParameterizedTypeReference<IdResponse>() {});

    String id = response == null ? "0" : String.valueOf(response.id());
    return new Product(id, input.name(), input.inventory(), input.type());
  }

  private record ProductResponse(int id, String name, int inventory, ProductType type) {
    Product toGraphqlProduct() {
      return new Product(String.valueOf(id), name, inventory, type);
    }
  }

  private record IdResponse(int id) {
  }
}
