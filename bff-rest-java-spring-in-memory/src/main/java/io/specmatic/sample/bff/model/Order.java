package io.specmatic.sample.bff.model;

public record Order(Integer id, Integer productid, Integer count, String status) {
}
