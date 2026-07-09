package io.specmatic.samples.bff;

import java.util.List;

public record ProductAuditMessage(
    String name,
    Integer inventory,
    Integer id,
    List<Category> categories
) {
  record Category(Integer id, String name) {
  }
}
