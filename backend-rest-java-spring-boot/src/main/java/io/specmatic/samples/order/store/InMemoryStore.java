package io.specmatic.samples.order.store;

import io.specmatic.samples.order.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryStore {
    private final Map<Integer, Product> products = new ConcurrentHashMap<>();
    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger productIds = new AtomicInteger(1000);
    private final AtomicInteger orderIds = new AtomicInteger(1000);

    public InMemoryStore() {
        products.put(10, new Product(10, "XYZ Phone", ProductType.gadget, 10, LocalDate.parse("2023-10-01")));
        products.put(20, new Product(20, "Book", ProductType.book, 5, LocalDate.parse("2023-10-01")));
        orders.put(10, new Order(10, 2, OrderStatus.pending, 10));
        orders.put(20, new Order(10, 1, OrderStatus.pending, 20));
    }

    public Product product(int id) { return products.get(id); }
    public ArrayList<Product> products() { return new ArrayList<>(products.values()); }
    public Product createProduct(ProductRequest request) {
        int id = productIds.incrementAndGet();
        Product product = new Product(id, request.name(), request.type(), request.inventory(), LocalDate.now());
        products.put(id, product);
        return product;
    }
    public void updateProduct(int id, ProductRequest request) {
        Product old = products.get(id);
        products.put(id, new Product(id, request.name(), request.type(), request.inventory(), old.createdOn()));
    }
    public Order order(int id) { return orders.get(id); }
    public ArrayList<Order> orders() { return new ArrayList<>(orders.values()); }
    public Order createOrder(OrderRequest request) {
        int id = orderIds.incrementAndGet();
        Order order = new Order(request.productid(), request.count(), OrderStatus.pending, id);
        orders.put(id, order);
        return order;
    }
    public void updateOrder(int id, OrderUpdate request) {
        orders.put(id, new Order(request.productid(), request.count(), request.status(), id));
    }
}
