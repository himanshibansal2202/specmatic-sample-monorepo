package io.specmatic.sample.store;

public record OrderResponse(
        Integer id,
        Integer productid,
        Integer count,
        OrderStatus status
) {
}
