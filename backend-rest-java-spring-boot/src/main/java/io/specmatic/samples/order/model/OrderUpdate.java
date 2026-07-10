package io.specmatic.samples.order.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderUpdate(
        @NotNull Integer productid,
        @NotNull @Min(1) Integer count,
        @NotNull OrderStatus status) {}
