package io.specmatic.sample.store;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryStore {
  private final InventoryClient inventoryClient;
  private final Map<Integer, Product> products = new ConcurrentHashMap<>();
  private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
  private final AtomicInteger nextProductId = new AtomicInteger(1000);
  private final AtomicInteger nextOrderId = new AtomicInteger(1000);

  public InMemoryStore(InventoryClient inventoryClient) {
    this.inventoryClient = inventoryClient;
    products.put(10, new Product(10, "XYZ Phone", "gadget", 10, "2023-10-01"));
    products.put(20, new Product(20, "Delete Me", "gadget", 10, "2023-10-01"));
    orders.put(10, new Order(10, 10, 2, "pending"));
    orders.put(20, new Order(20, 10, 1, "pending"));
  }

  public Optional<Product> findProduct(int id) {
    return Optional.ofNullable(products.get(id))
        .map(product -> new Product(product.id(), product.name(), product.type(), inventoryClient.getInventory(id), product.createdOn()));
  }

  public List<Product> findProducts(String type, String fromDate, String toDate, Integer pageSize) {
    return products.values().stream()
        .map(product -> new Product(product.id(), product.name(), product.type(), inventoryClient.getInventory(product.id()), product.createdOn()))
        .filter(product -> type == null || product.type().equals(type))
        .filter(product -> fromDate == null || !LocalDate.parse(product.createdOn()).isBefore(LocalDate.parse(fromDate)))
        .filter(product -> toDate == null || !LocalDate.parse(product.createdOn()).isAfter(LocalDate.parse(toDate)))
        .sorted(Comparator.comparingInt(Product::id))
        .limit(pageSize == null ? Long.MAX_VALUE : Math.max(pageSize, 0))
        .toList();
  }

  public Product createProduct(ProductBase request) {
    int id = nextProductId.getAndIncrement();
    Product product = new Product(id, request.name(), request.type(), request.inventory(), LocalDate.now().toString());
    products.put(id, product);
    inventoryClient.addInventory(id, request.inventory());
    return product;
  }

  public Optional<Product> updateProduct(int id, ProductBase request) {
    if (!products.containsKey(id)) {
      return Optional.empty();
    }
    Product updated = products.get(id).update(request);
    products.put(id, updated);
    inventoryClient.addInventory(id, request.inventory());
    return Optional.of(updated);
  }

  public boolean deleteProduct(int id) {
    return products.remove(id) != null;
  }

  public List<Order> findOrders() {
    return new ArrayList<>(orders.values()).stream()
        .sorted(Comparator.comparingInt(Order::id))
        .toList();
  }

  public Optional<Order> findOrder(int id) {
    return Optional.ofNullable(orders.get(id));
  }

  public Optional<Order> createOrder(OrderBase request) {
    if (!products.containsKey(request.productid()) || !inventoryClient.reduceInventory(request.productid(), request.count())) {
      return Optional.empty();
    }
    int id = nextOrderId.getAndIncrement();
    Order order = new Order(id, request.productid(), request.count(), "pending");
    orders.put(id, order);
    return Optional.of(order);
  }

  public Optional<Order> updateOrder(int id, OrderUpdate request) {
    if (!orders.containsKey(id)) {
      return Optional.empty();
    }
    Order updated = orders.get(id).update(request);
    orders.put(id, updated);
    return Optional.of(updated);
  }

  public boolean deleteOrder(int id) {
    Order existing = orders.get(id);
    if (existing == null) {
      return false;
    }
    orders.put(id, new Order(id, existing.productid(), existing.count(), "cancelled"));
    return true;
  }
}
