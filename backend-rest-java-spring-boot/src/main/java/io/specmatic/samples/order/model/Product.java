package io.specmatic.samples.order.model;

import java.time.LocalDate;

public record Product(int id, String name, ProductType type, int inventory, LocalDate createdOn) {}
