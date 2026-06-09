package io.specmatic.sample.bff.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ProductBase(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "book|food|gadget|other") String type,
        @NotNull @Min(1) @Max(101) Integer inventory
) {
}
