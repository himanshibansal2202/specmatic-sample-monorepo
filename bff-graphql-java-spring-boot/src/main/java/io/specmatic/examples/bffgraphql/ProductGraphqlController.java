package io.specmatic.examples.bffgraphql;

import java.time.LocalDate;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProductGraphqlController {
  private final BackendProductClient backendProductClient;

  public ProductGraphqlController(BackendProductClient backendProductClient) {
    this.backendProductClient = backendProductClient;
  }

  @QueryMapping
  public List<Product> findAvailableProducts(@Argument ProductType type, @Argument Integer pageSize) {
    return backendProductClient.findAvailableProducts(type, pageSize);
  }

  @QueryMapping
  public List<Offer> findOffersForDate(@Argument LocalDate date) {
    return List.of(
        new Offer("WKND30", LocalDate.parse("2024-12-12")),
        new Offer("SUNDAY20", LocalDate.parse("2024-12-25")));
  }

  @MutationMapping
  public Product createProduct(@Argument NewProductInput newProduct) {
    return backendProductClient.createProduct(newProduct);
  }
}
