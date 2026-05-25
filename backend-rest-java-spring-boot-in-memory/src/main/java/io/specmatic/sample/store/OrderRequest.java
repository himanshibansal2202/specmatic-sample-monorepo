package io.specmatic.sample.store;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotNull Integer productid,
        @NotNull @Positive Integer count
) {
}
