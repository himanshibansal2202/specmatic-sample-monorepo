package io.specmatic.samples.bff;

public record Order(
    Integer id,
    Integer productid,
    Integer count,
    String status
) {
}
