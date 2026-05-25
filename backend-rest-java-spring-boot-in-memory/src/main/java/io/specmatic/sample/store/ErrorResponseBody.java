package io.specmatic.sample.store;

public record ErrorResponseBody(String timestamp, int status, String error, String message) {
}
