package io.specmatic.examples.store.model;

public record Order(
        Integer productid,
        Integer count,
        OrderStatus status,
        Integer id
) {
    public Order withUpdate(OrderUpdate update) {
        return new Order(update.productid(), update.count(), update.status(), id);
    }
}
