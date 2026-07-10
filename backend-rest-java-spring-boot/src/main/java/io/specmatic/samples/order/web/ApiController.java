package io.specmatic.samples.order.web;

import io.specmatic.samples.order.model.*;
import io.specmatic.samples.order.service.InventoryService;
import io.specmatic.samples.order.store.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
public class ApiController {
    private final InMemoryStore store;
    private final InventoryService inventory;

    public ApiController(InMemoryStore store, InventoryService inventory) {
        this.store = store;
        this.inventory = inventory;
    }

    @GetMapping("/products/{id}")
    public Product getProduct(@PathVariable int id) {
        Product product = requireProduct(id);
        return new Product(product.id(), product.name(), product.type(),
                inventory.getInventory(id, product.inventory()), product.createdOn());
    }

    @PatchMapping(value = "/products/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String updateProduct(@PathVariable int id, @RequestHeader("Authenticate") String apiKey,
                                @Valid @RequestBody ProductRequest request) {
        requireProduct(id);
        store.updateProduct(id, request);
        inventory.addInventory(id, request.inventory());
        return "success";
    }

    @DeleteMapping(value = "/products/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteProduct(@PathVariable int id, @RequestHeader("Authenticate") String apiKey) {
        requireProduct(id);
        return "success";
    }

    @PutMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateImage(@PathVariable int id, @RequestPart("image") MultipartFile image) {
        requireProduct(id);
        if (image.isEmpty()) throw new BadRequestException("Image is required");
        return ResponseEntity.ok(java.util.Map.of("message", "Success"));
    }

    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Product> products(@RequestParam(required = false) ProductType type,
                                  @RequestHeader(value = "pageSize", required = false) Integer pageSize,
                                  @RequestParam(value = "from-date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(value = "to-date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return store.products().stream()
                .filter(p -> type == null || p.type() == type)
                .filter(p -> from == null || !p.createdOn().isBefore(from))
                .filter(p -> to == null || !p.createdOn().isAfter(to))
                .limit(pageSize == null ? Long.MAX_VALUE : pageSize)
                .toList();
    }

    @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IdResponse> createProduct(@RequestHeader("Authenticate") String apiKey,
                                                     @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                     @Valid @RequestBody ProductRequest request) {
        Product product = store.createProduct(request);
        inventory.addInventory(product.id(), request.inventory());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(product.id()));
    }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IdResponse> createOrder(@RequestHeader("Authenticate") String apiKey,
                                                   @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                   @Valid @RequestBody OrderRequest request) {
        Order order = store.createOrder(request);
        inventory.reduceInventory(request.productid(), request.count());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(order.id()));
    }

    @GetMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Order> orders() { return store.orders(); }

    @GetMapping(value = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Order getOrder(@PathVariable int id) { return requireOrder(id); }

    @PatchMapping(value = "/orders/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String updateOrder(@PathVariable int id, @RequestHeader("Authenticate") String apiKey,
                              @Valid @RequestBody OrderUpdate request) {
        requireOrder(id);
        store.updateOrder(id, request);
        return "success";
    }

    @DeleteMapping(value = "/orders/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteOrder(@PathVariable int id, @RequestHeader("Authenticate") String apiKey) {
        requireOrder(id);
        return "success";
    }

    private Product requireProduct(int id) {
        Product product = store.product(id);
        if (product == null) throw new ResourceNotFoundException("Product not found");
        return product;
    }

    private Order requireOrder(int id) {
        Order order = store.order(id);
        if (order == null) throw new ResourceNotFoundException("Order not found");
        return order;
    }
}
