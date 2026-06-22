package io.specmatic.examples.store.api;

import io.specmatic.examples.store.model.IdResponse;
import io.specmatic.examples.store.model.ImageUpdateResponse;
import io.specmatic.examples.store.model.Product;
import io.specmatic.examples.store.model.ProductType;
import io.specmatic.examples.store.service.StoreService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProductController {
    private final StoreService storeService;

    public ProductController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping(path = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Product getProduct(@PathVariable int id) {
        return storeService.getProduct(id);
    }

    @PatchMapping(
            path = "/products/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String updateProduct(
            @PathVariable int id,
            @RequestHeader(value = "Authenticate", required = false) String authenticate,
            @RequestBody Map<String, Object> request) {
        storeService.updateProduct(id, RequestParser.productBase(request));
        return "success";
    }

    @DeleteMapping(path = "/products/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteProduct(
            @PathVariable int id,
            @RequestHeader(value = "Authenticate", required = false) String authenticate) {
        storeService.deleteProduct(id);
        return "success";
    }

    @PutMapping(
            path = "/products/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ImageUpdateResponse updateProductImage(@PathVariable int id, @RequestParam("image") MultipartFile image) {
        storeService.updateProductImage(id);
        return new ImageUpdateResponse("Success");
    }

    @GetMapping(path = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Product> getProducts(
            @RequestParam(value = "type", required = false) ProductType type,
            @RequestHeader(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "from-date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to-date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return storeService.searchProducts(type, fromDate, toDate, pageSize);
    }

    @PostMapping(
            path = "/products",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createProduct(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestHeader(value = "Authenticate", required = false) String authenticate,
            @RequestBody Map<String, Object> request) {
        return storeService.createProduct(RequestParser.productBase(request));
    }
}
