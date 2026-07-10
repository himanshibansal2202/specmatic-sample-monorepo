package io.specmatic.samples.order.web;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
