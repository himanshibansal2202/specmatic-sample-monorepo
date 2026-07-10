package io.specmatic.samples.order.model;

public record Order(int productid, int count, OrderStatus status, int id) {}
