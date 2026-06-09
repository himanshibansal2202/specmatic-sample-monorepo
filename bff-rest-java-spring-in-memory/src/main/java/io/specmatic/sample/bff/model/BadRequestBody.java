package io.specmatic.sample.bff.model;

public record BadRequestBody(String timestamp, int status, String error, String message) {
}
