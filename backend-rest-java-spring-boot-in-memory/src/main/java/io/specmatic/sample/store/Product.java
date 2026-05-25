package io.specmatic.sample.store;

public record Product(int id, String name, String type, int inventory, String createdOn) {
  Product update(ProductBase update) {
    return new Product(id, update.name(), update.type(), update.inventory(), createdOn);
  }
}
