package io.specmatic.samples.bff;

public record ErrorResponse(
    String timestamp,
    Integer status,
    String error,
    String message
) {
}
