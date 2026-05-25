package io.specmatic.sample.store;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderUpdateRequest(
        @NotNull Integer productid,
        @NotNull @Positive Integer count,
        @NotNull OrderStatus status
) {
}
