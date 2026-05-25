package io.specmatic.sample.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
public class StoreController {
    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    @GetMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProductResponse getProduct(@PathVariable int id) {
        return service.getProduct(id);
    }

    @PatchMapping(value = "/products/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String updateProduct(@PathVariable int id,
                                @RequestHeader(name = "Authenticate", required = false) String authenticate,
                                @RequestBody JsonNode request) {
        service.updateProduct(id, toProductRequest(request));
        return "success";
    }

    @DeleteMapping(value = "/products/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteProduct(@PathVariable int id,
                                @RequestHeader(name = "Authenticate", required = false) String authenticate) {
        service.deleteProduct(id);
        return "success";
    }

    @PutMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ImageResponse updateProductImage(@PathVariable int id, @RequestPart("image") MultipartFile image) {
        service.updateProductImage(id);
        return new ImageResponse("Success");
    }

    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProductResponse> getProducts(@RequestParam(required = false) ProductType type,
                                             @RequestHeader(name = "pageSize", required = false) Integer pageSize,
                                             @RequestParam(name = "from-date", required = false) LocalDate fromDate,
                                             @RequestParam(name = "to-date", required = false) LocalDate toDate) {
        return service.getProducts(type);
    }

    @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createProduct(@RequestHeader(name = "Authenticate", required = false) String authenticate,
                                    @RequestHeader(name = "Idempotency-Key") @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String idempotencyKey,
                                    @RequestBody JsonNode request) {
        return service.createProduct(toProductRequest(request));
    }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createOrder(@RequestHeader(name = "Authenticate", required = false) String authenticate,
                                  @RequestHeader(name = "Idempotency-Key") @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String idempotencyKey,
                                  @Valid @RequestBody OrderRequest request) {
        return service.createOrder(request);
    }

    @GetMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderResponse> getOrders() {
        return service.getOrders();
    }

    @GetMapping(value = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponse getOrder(@PathVariable int id) {
        return service.getOrder(id);
    }

    @PatchMapping(value = "/orders/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String updateOrder(@PathVariable int id,
                              @RequestHeader(name = "Authenticate", required = false) String authenticate,
                              @Valid @RequestBody OrderUpdateRequest request) {
        service.updateOrder(id, request);
        return "success";
    }

    @DeleteMapping(value = "/orders/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteOrder(@PathVariable int id,
                              @RequestHeader(name = "Authenticate", required = false) String authenticate) {
        service.deleteOrder(id);
        return "success";
    }

    private ProductRequest toProductRequest(JsonNode request) {
        if (request == null || !request.isObject()) {
            throw new IllegalArgumentException("Product request body must be an object");
        }

        JsonNode name = request.get("name");
        JsonNode type = request.get("type");
        JsonNode inventory = request.get("inventory");

        if (name == null || !name.isTextual() || name.asText().isBlank()) {
            throw new IllegalArgumentException("Product name must be a string");
        }
        if (type == null || !type.isTextual()) {
            throw new IllegalArgumentException("Product type must be a string");
        }
        if (inventory == null || !inventory.isIntegralNumber() || inventory.intValue() < 1 || inventory.intValue() > 101) {
            throw new IllegalArgumentException("Product inventory must be an integer between 1 and 101");
        }

        try {
            return new ProductRequest(name.asText(), ProductType.valueOf(type.asText()), inventory.intValue());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Product type must match the contract enum", exception);
        }
    }
}
