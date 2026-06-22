package io.specmatic.examples.store.api;

import io.specmatic.examples.store.model.IdResponse;
import io.specmatic.examples.store.model.Order;
import io.specmatic.examples.store.service.StoreService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final StoreService storeService;

    public OrderController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping(
            path = "/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createOrder(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestHeader(value = "Authenticate", required = false) String authenticate,
            @RequestBody Map<String, Object> request) {
        return storeService.createOrder(RequestParser.orderBase(request));
    }

    @GetMapping(path = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Order> getOrders() {
        return storeService.searchOrders();
    }

    @GetMapping(path = "/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Order getOrder(@PathVariable int id) {
        return storeService.getOrder(id);
    }

    @PatchMapping(
            path = "/orders/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String updateOrder(
            @PathVariable int id,
            @RequestHeader(value = "Authenticate", required = false) String authenticate,
            @RequestBody Map<String, Object> request) {
        storeService.updateOrder(id, RequestParser.orderUpdate(request));
        return "success";
    }

    @DeleteMapping(path = "/orders/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteOrder(
            @PathVariable int id,
            @RequestHeader(value = "Authenticate", required = false) String authenticate) {
        storeService.deleteOrder(id);
        return "success";
    }
}
