package io.specmatic.sample.bff.web;

import io.specmatic.sample.bff.model.HeaderItem;
import io.specmatic.sample.bff.model.IdResponse;
import io.specmatic.sample.bff.model.MonitorResponse;
import io.specmatic.sample.bff.model.Order;
import io.specmatic.sample.bff.model.OrderBase;
import io.specmatic.sample.bff.model.Product;
import io.specmatic.sample.bff.model.ProductBase;
import io.specmatic.sample.bff.service.BackendClient;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@Validated
public class BffController {
    private static final Set<String> PRODUCT_TYPES = Set.of("book", "food", "gadget", "other");
    private final AtomicReference<Map<String, Object>> monitorRequestBody = new AtomicReference<>(Map.of());
    private final BackendClient backendClient;

    public BffController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @PostMapping("/products")
    public ResponseEntity<IdResponse> createProduct(
            @RequestHeader(value = "Specmatic-Response-Code", required = false) Integer responseCode,
            @RequestBody(required = false) JsonNode requestBody) {
        ProductBase product = parseProduct(requestBody);
        if (Integer.valueOf(202).equals(responseCode)) {
            monitorRequestBody.set(Map.of(
                    "name", product.name(),
                    "type", product.type(),
                    "inventory", product.inventory()
            ));
            return ResponseEntity.accepted().header(HttpHeaders.LINK, "</monitor/123>;rel=related;title=monitor").build();
        }
        ResponseEntity<IdResponse> response = backendClient.createProduct(product);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/findAvailableProducts")
    public ResponseEntity<List<Product>> findAvailableProducts(
            @RequestHeader(value = "Specmatic-Response-Code", required = false) Integer responseCode,
            @RequestParam(required = false) @Pattern(regexp = "book|food|gadget|other") String type,
            @RequestHeader("pageSize") Integer pageSize,
            @RequestParam("from-date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to-date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        if (Integer.valueOf(429).equals(responseCode)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "30").build();
        }
        return ResponseEntity.ok(backendClient.findAvailableProducts(type, pageSize, fromDate, toDate));
    }

    @PostMapping("/orders")
    public ResponseEntity<IdResponse> createOrder(
            @RequestHeader(value = "Specmatic-Response-Code", required = false) Integer responseCode,
            @RequestBody(required = false) JsonNode requestBody) {
        OrderBase order = parseOrder(requestBody);
        if (Integer.valueOf(202).equals(responseCode)) {
            monitorRequestBody.set(Map.of(
                    "productid", order.productid(),
                    "count", order.count()
            ));
            return ResponseEntity.accepted().header(HttpHeaders.LINK, "</monitor/123>;rel=related;title=monitor").build();
        }
        ResponseEntity<IdResponse> response = backendClient.createOrder(order);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) Integer orderId) {
        return ResponseEntity.ok(backendClient.getOrders());
    }

    @GetMapping("/monitor/{id}")
    public ResponseEntity<MonitorResponse> retrieveMonitor(@PathVariable Integer id) {
        MonitorResponse body = new MonitorResponse(
                new MonitorResponse.MonitorRequest("POST", monitorRequestBody.get(), List.of(new HeaderItem("id", id.toString()))),
                new MonitorResponse.MonitorResult(201, Map.of("id", 1), List.of(new HeaderItem("content-type", "application/json")))
        );
        return ResponseEntity.ok(body);
    }

    private ProductBase parseProduct(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("Product request body is required");
        }
        JsonNode name = body.get("name");
        JsonNode type = body.get("type");
        JsonNode inventory = body.get("inventory");
        if (name == null || !name.isTextual() || name.asString().isBlank()) {
            throw new IllegalArgumentException("Product name must be a string");
        }
        if (type == null || !type.isTextual() || !PRODUCT_TYPES.contains(type.asString())) {
            throw new IllegalArgumentException("Product type must be one of book, food, gadget, other");
        }
        if (inventory == null || !inventory.isIntegralNumber()) {
            throw new IllegalArgumentException("Product inventory must be an integer");
        }
        int inventoryValue = inventory.asInt();
        if (inventoryValue < 1 || inventoryValue > 101) {
            throw new IllegalArgumentException("Product inventory must be between 1 and 101");
        }
        return new ProductBase(name.asString(), type.asString(), inventoryValue);
    }

    private OrderBase parseOrder(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("Order request body is required");
        }
        JsonNode productId = body.get("productid");
        JsonNode count = body.get("count");
        if (productId == null || !productId.isIntegralNumber()) {
            throw new IllegalArgumentException("Order productid must be an integer");
        }
        if (count == null || !count.isIntegralNumber()) {
            throw new IllegalArgumentException("Order count must be an integer");
        }
        return new OrderBase(productId.asInt(), count.asInt());
    }
}
