package io.specmatic.sample.store;

public record ProductResponse(
        Integer id,
        String name,
        ProductType type,
        Integer inventory,
        String createdOn
) {
}
