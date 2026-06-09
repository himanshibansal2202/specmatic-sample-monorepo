package io.specmatic.sample.bff.model;

public record Product(Integer id, String name, String type, Integer inventory, String createdOn) {
}
