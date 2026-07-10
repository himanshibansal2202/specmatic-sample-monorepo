package io.specmatic.samples.order.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        @NotNull ProductType type,
        @Min(1) @Max(101) Integer inventory) {}
