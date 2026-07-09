package io.specmatic.samples.bff;

import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class ProductAuditPublisher {
  private final KafkaTemplate<String, ProductAuditMessage> kafkaTemplate;

  ProductAuditPublisher(KafkaTemplate<String, ProductAuditMessage> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  void publishCreatedProduct(ProductBase request, Integer id) {
    var message = new ProductAuditMessage(
        request.name(),
        request.inventory(),
        id,
        List.of(new ProductAuditMessage.Category(1, request.type().name()))
    );
    kafkaTemplate.send("product-queries", String.valueOf(id), message);
  }
}
