package io.specmatic.samples.bff;

import jakarta.validation.constraints.NotNull;

public record OrderBase(
    @NotNull Integer productid,
    @NotNull Integer count
) {
}
