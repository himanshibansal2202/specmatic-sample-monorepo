package io.specmatic.sample.store;

public record ApiError(
        String timestamp,
        Integer status,
        String error,
        String message
) {
}
