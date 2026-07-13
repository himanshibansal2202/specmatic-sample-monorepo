package io.specmatic.examples.bffgraphql;

public record NewProductInput(String name, int inventory, ProductType type) {
}
