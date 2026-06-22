package io.specmatic.examples.store.service;

public class UnprocessableRequestException extends RuntimeException {
    public UnprocessableRequestException(String message) {
        super(message);
    }
}
