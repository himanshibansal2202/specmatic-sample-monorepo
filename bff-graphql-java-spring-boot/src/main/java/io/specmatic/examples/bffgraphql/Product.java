package io.specmatic.examples.bffgraphql;

public record Product(String id, String name, int inventory, ProductType type) {
}
