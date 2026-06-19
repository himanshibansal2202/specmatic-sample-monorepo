package io.specmatic.examples.bff;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
class BffController {
  private final BackendClient backendClient;
  private final Map<Integer, MonitorResponse> monitors = new ConcurrentHashMap<>();

  BffController(BackendClient backendClient) {
    this.backendClient = backendClient;
  }

  @PostMapping("/products")
  ResponseEntity<Id> createProduct(
      @Valid @RequestBody ProductBase product,
      @RequestHeader(value = "Specmatic-Response-Code", required = false) String requestedResponseCode) {
    validateProductName(product.name());
    if ("202".equals(requestedResponseCode)) {
      monitors.put(123, acceptedMonitor("POST", Map.of(
          "name", product.name(),
          "type", product.type().name(),
          "inventory", product.inventory())));
      return ResponseEntity.accepted().header(HttpHeaders.LINK, "</monitor/123>;rel=related;title=monitor").build();
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(backendClient.createProduct(product));
  }

  @GetMapping("/findAvailableProducts")
  List<Product> findAvailableProducts(
      @RequestParam(required = false) ProductType type,
      @RequestHeader @NotNull Integer pageSize,
      @RequestParam("from-date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam("to-date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    return backendClient.findAvailableProducts(type, pageSize, fromDate, toDate);
  }

  @PostMapping("/orders")
  ResponseEntity<Id> createOrder(
      @Valid @RequestBody OrderBase order,
      @RequestHeader(value = "Specmatic-Response-Code", required = false) String requestedResponseCode) {
    if ("202".equals(requestedResponseCode)) {
      monitors.put(123, acceptedMonitor("POST", Map.of(
          "productid", order.productid(),
          "count", order.count())));
      return ResponseEntity.accepted().header(HttpHeaders.LINK, "</monitor/123>;rel=related;title=monitor").build();
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(backendClient.createOrder(order));
  }

  @GetMapping("/orders")
  List<Order> getOrders(@RequestParam(required = false) Integer orderId) {
    if (orderId == null) {
      return backendClient.getOrders().stream().map(this::toBffOrder).toList();
    }
    return List.of(toBffOrder(backendClient.getOrder(orderId)));
  }

  @GetMapping("/monitor/{id}")
  MonitorResponse retrieveMonitor(@PathVariable Integer id) {
    MonitorResponse monitor = monitors.get(id);
    if (monitor != null) {
      return monitor;
    }
    return new MonitorResponse(
        new MonitorRequest("GET", Map.of("id", id), List.of(new HeaderItem("Accept", "application/json"))),
        new MonitorResult(200, Map.of("status", "completed"), List.of(new HeaderItem(HttpHeaders.CONTENT_TYPE, "application/json"))));
  }

  private Order toBffOrder(Order order) {
    String status = "fulfilled".equals(order.status()) ? "completed" : order.status();
    return new Order(order.id(), order.productid(), order.count(), status);
  }

  private MonitorResponse acceptedMonitor(String method, Map<String, Object> requestBody) {
    return new MonitorResponse(
        new MonitorRequest(method, requestBody, List.of(new HeaderItem(HttpHeaders.CONTENT_TYPE, "application/json"))),
        new MonitorResult(201, Map.of("id", 123), List.of(new HeaderItem(HttpHeaders.CONTENT_TYPE, "application/json"))));
  }

  private void validateProductName(Object name) {
    if (!(name instanceof String value) || value.isBlank()) {
      throw new IllegalArgumentException("Product name must be a non-empty string");
    }
  }
}
