package io.specmatic.sample.store;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        @NotNull ProductType type,
        @NotNull @Min(1) @Max(101) Integer inventory
) {
}
