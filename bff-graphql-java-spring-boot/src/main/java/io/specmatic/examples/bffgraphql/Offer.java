package io.specmatic.examples.bffgraphql;

import java.time.LocalDate;

public record Offer(String offerCode, LocalDate validUntil) {
}
