package io.specmatic.sample.store;

public record Order(int id, int productid, int count, String status) {
  Order update(OrderUpdate update) {
    return new Order(id, update.productid(), update.count(), update.status());
  }
}
