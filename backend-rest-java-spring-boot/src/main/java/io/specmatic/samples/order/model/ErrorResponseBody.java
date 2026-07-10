package io.specmatic.samples.order.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseBody(String timestamp, Integer status, String error, String message) {}
