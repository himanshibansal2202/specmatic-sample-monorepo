package io.specmatic.samples.bff;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class BffController {
  private static final String SPEC_RESPONSE_CODE = "Specmatic-Response-Code";

  private final RestTemplate restTemplate;
  private final ProductAuditPublisher productAuditPublisher;
  private final AtomicInteger monitorIds = new AtomicInteger(122);
  private final Map<Integer, MonitorResponse> monitors = new ConcurrentHashMap<>();

  BffController(RestTemplate restTemplate, ProductAuditPublisher productAuditPublisher) {
    this.restTemplate = restTemplate;
    this.productAuditPublisher = productAuditPublisher;
  }

  @PostMapping(path = "/products", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body,
                                  @RequestHeader(value = SPEC_RESPONSE_CODE, required = false) String responseCode) {
    ProductBase request = productBase(body);
    if ("202".equals(responseCode)) {
      int monitorId = monitorIds.incrementAndGet();
      monitors.put(monitorId, acceptedMonitor("POST", body));
      return ResponseEntity.accepted().header(HttpHeaders.LINK, "</monitor/" + monitorId + ">;rel=related;title=monitor").build();
    }

    var dependencyResponse = restTemplate.postForEntity("/products", new HttpEntity<>(request, dependencyHeaders()), IdResponse.class);
    var id = dependencyResponse.getBody() == null ? 1 : dependencyResponse.getBody().id();
    productAuditPublisher.publishCreatedProduct(request, id);
    return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
  }

  @GetMapping(path = "/findAvailableProducts", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> findAvailableProducts(@RequestParam(required = false) ProductType type,
                                          @RequestParam("from-date") String fromDate,
                                          @RequestParam("to-date") String toDate,
                                          @RequestHeader("pageSize") Integer pageSize,
                                          @RequestHeader(value = SPEC_RESPONSE_CODE, required = false) String responseCode) {
    if ("429".equals(responseCode)) {
      return ResponseEntity.status(429).header("Retry-After", "1").build();
    }

    String uri = UriComponentsBuilder.fromPath("/products")
        .queryParamIfPresent("type", java.util.Optional.ofNullable(type).map(ProductType::name))
        .queryParam("from-date", fromDate)
        .queryParam("to-date", toDate)
        .build()
        .toUriString();
    var headers = new HttpHeaders();
    headers.set("pageSize", String.valueOf(pageSize));
    var dependencyResponse = restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers),
        new ParameterizedTypeReference<List<Product>>() {
        });
    return ResponseEntity.ok(dependencyResponse.getBody());
  }

  @PostMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body,
                                @RequestHeader(value = SPEC_RESPONSE_CODE, required = false) String responseCode) {
    OrderBase request = orderBase(body);
    if ("202".equals(responseCode)) {
      int monitorId = monitorIds.incrementAndGet();
      monitors.put(monitorId, acceptedMonitor("POST", body));
      return ResponseEntity.accepted().header(HttpHeaders.LINK, "</monitor/" + monitorId + ">;rel=related;title=monitor").build();
    }

    var dependencyResponse = restTemplate.postForEntity("/orders", new HttpEntity<>(request, dependencyHeaders()), IdResponse.class);
    var id = dependencyResponse.getBody() == null ? 1 : dependencyResponse.getBody().id();
    return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
  }

  @GetMapping(path = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) Integer orderId) {
    var dependencyResponse = restTemplate.exchange("/orders", org.springframework.http.HttpMethod.GET, HttpEntity.EMPTY,
        new ParameterizedTypeReference<List<Order>>() {
        });
    var orders = dependencyResponse.getBody() == null ? List.<Order>of() : dependencyResponse.getBody();
    if (orderId == null) {
      return ResponseEntity.ok(orders);
    }
    return ResponseEntity.ok(orders.stream().filter(order -> orderId.equals(order.id())).toList());
  }

  @GetMapping(path = "/monitor/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  MonitorResponse retrieveMonitor(@PathVariable Integer id) {
    return monitors.getOrDefault(id,
        new MonitorResponse(
            new MonitorRequest("GET", Map.of("id", id), List.of(new HeaderItem("Accept", "application/json"))),
            new MonitorBackendResponse(201, Map.of("id", id), List.of(new HeaderItem("Content-Type", "application/json")))
        ));
  }

  private MonitorResponse acceptedMonitor(String method, Map<String, Object> requestBody) {
    return new MonitorResponse(
        new MonitorRequest(method, requestBody, List.of(new HeaderItem("Content-Type", "application/json"))),
        new MonitorBackendResponse(201, Map.of("id", 123), List.of(new HeaderItem("Content-Type", "application/json")))
    );
  }

  private HttpHeaders dependencyHeaders() {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authenticate", "sample-api-key");
    headers.set("Idempotency-Key", UUID.randomUUID().toString());
    return headers;
  }

  private ProductBase productBase(Map<String, Object> body) {
    Object name = body.get("name");
    Object type = body.get("type");
    Object inventory = body.get("inventory");
    if (!(name instanceof String nameValue) || nameValue.isBlank()) {
      throw new IllegalArgumentException("name must be a string");
    }
    if (!(type instanceof String typeValue)) {
      throw new IllegalArgumentException("type must be a string");
    }
    ProductType productType = ProductType.valueOf(typeValue);
    int inventoryValue = integerValue(inventory, "inventory");
    if (inventoryValue < 1 || inventoryValue > 101) {
      throw new IllegalArgumentException("inventory must be between 1 and 101");
    }
    return new ProductBase(nameValue, productType, inventoryValue);
  }

  private OrderBase orderBase(Map<String, Object> body) {
    return new OrderBase(
        integerValue(body.get("productid"), "productid"),
        integerValue(body.get("count"), "count")
    );
  }

  private int integerValue(Object value, String field) {
    if (!(value instanceof Number number) || value instanceof Float || value instanceof Double) {
      throw new IllegalArgumentException(field + " must be an integer");
    }
    return number.intValue();
  }
}
