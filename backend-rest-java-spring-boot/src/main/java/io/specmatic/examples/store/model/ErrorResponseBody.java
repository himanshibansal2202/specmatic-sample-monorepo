package io.specmatic.examples.store.model;

public record ErrorResponseBody(
        String timestamp,
        Integer status,
        String error,
        String message
) {
}
