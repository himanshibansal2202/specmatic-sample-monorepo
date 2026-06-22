package io.specmatic.examples.store.model;

public record Product(
        Integer id,
        String name,
        ProductType type,
        Integer inventory,
        String createdOn
) {
    public Product withBase(ProductBase update) {
        return new Product(id, update.name(), update.type(), update.inventory(), createdOn);
    }
}
