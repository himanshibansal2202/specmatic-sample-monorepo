package io.specmatic.sample.store;

public record OrderUpdate(int productid, int count, String status) {
}
