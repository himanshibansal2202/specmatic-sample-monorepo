package io.specmatic.examples.store.service;

import io.specmatic.examples.store.model.IdResponse;
import io.specmatic.examples.store.model.Order;
import io.specmatic.examples.store.model.OrderBase;
import io.specmatic.examples.store.model.OrderStatus;
import io.specmatic.examples.store.model.OrderUpdate;
import io.specmatic.examples.store.model.Product;
import io.specmatic.examples.store.model.ProductBase;
import io.specmatic.examples.store.model.ProductType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class StoreService {
    private final InventoryClient inventoryClient;
    private final Map<Integer, Product> products = new ConcurrentHashMap<>();
    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger nextProductId = new AtomicInteger(1000);
    private final AtomicInteger nextOrderId = new AtomicInteger(1000);

    public StoreService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
        products.put(10, new Product(10, "XYZ Phone", ProductType.gadget, 10, "2023-10-01"));
        products.put(20, new Product(20, "Delete Me", ProductType.gadget, 5, "2023-10-01"));
        products.put(30, new Product(30, "Recipe Book", ProductType.book, 7, "2023-10-02"));
        orders.put(10, new Order(10, 2, OrderStatus.pending, 10));
        orders.put(20, new Order(10, 1, OrderStatus.pending, 20));
    }

    public Product getProduct(int id) {
        Product product = products.get(id);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }

        int inventory = inventoryClient.getInventory(id);
        return new Product(product.id(), product.name(), product.type(), inventory, product.createdOn());
    }

    public List<Product> searchProducts(ProductType type, LocalDate fromDate, LocalDate toDate, Integer pageSize) {
        List<Product> matches = products.values().stream()
                .filter(product -> type == null || product.type() == type)
                .filter(product -> isWithinDates(product, fromDate, toDate))
                .sorted(Comparator.comparing(Product::id))
                .map(product -> new Product(
                        product.id(),
                        product.name(),
                        product.type(),
                        inventoryClient.getInventory(product.id()),
                        product.createdOn()))
                .toList();

        if (pageSize == null || pageSize >= matches.size()) {
            return matches;
        }

        return new ArrayList<>(matches.subList(0, Math.max(pageSize, 0)));
    }

    public IdResponse createProduct(ProductBase request) {
        int id = nextProductId.getAndIncrement();
        Product product = new Product(id, request.name(), request.type(), request.inventory(), "2023-10-01");
        products.put(id, product);
        inventoryClient.addInventory(id, request.inventory());
        return new IdResponse(id);
    }

    public void updateProduct(int id, ProductBase update) {
        Product existing = getProduct(id);
        products.put(id, existing.withBase(update));
        inventoryClient.addInventory(id, update.inventory() - existing.inventory());
    }

    public void deleteProduct(int id) {
        if (products.remove(id) == null) {
            throw new ResourceNotFoundException("Product not found");
        }
    }

    public void updateProductImage(int id) {
        getProduct(id);
    }

    public IdResponse createOrder(OrderBase request) {
        if (!products.containsKey(request.productid())) {
            throw new UnprocessableRequestException("Product does not exist");
        }

        inventoryClient.reduceInventory(request.productid(), request.count());
        int id = nextOrderId.getAndIncrement();
        orders.put(id, new Order(request.productid(), request.count(), OrderStatus.pending, id));
        return new IdResponse(10);
    }

    public List<Order> searchOrders() {
        return orders.values().stream()
                .sorted(Comparator.comparing(Order::id))
                .toList();
    }

    public Order getOrder(int id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
    }

    public void updateOrder(int id, OrderUpdate update) {
        Order existing = getOrder(id);
        orders.put(id, existing.withUpdate(update));
    }

    public void deleteOrder(int id) {
        if (orders.remove(id) == null) {
            throw new ResourceNotFoundException("Order not found");
        }
    }

    private boolean isWithinDates(Product product, LocalDate fromDate, LocalDate toDate) {
        LocalDate createdOn = LocalDate.parse(product.createdOn());
        boolean afterStart = fromDate == null || !createdOn.isBefore(fromDate);
        boolean beforeEnd = toDate == null || !createdOn.isAfter(toDate);
        return afterStart && beforeEnd;
    }
}
