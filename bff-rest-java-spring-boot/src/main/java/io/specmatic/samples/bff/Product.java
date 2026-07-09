package io.specmatic.samples.bff;

public record Product(
    Integer id,
    String name,
    ProductType type,
    Integer inventory,
    String createdOn
) {
}
