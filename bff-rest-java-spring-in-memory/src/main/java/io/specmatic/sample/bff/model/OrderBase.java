package io.specmatic.sample.bff.model;

import jakarta.validation.constraints.NotNull;

public record OrderBase(@NotNull Integer productid, @NotNull Integer count) {
}
