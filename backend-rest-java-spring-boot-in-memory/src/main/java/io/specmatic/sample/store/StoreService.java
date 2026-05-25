package io.specmatic.sample.store;

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
    private final Map<Integer, ProductResponse> products = new ConcurrentHashMap<>();
    private final Map<Integer, OrderResponse> orders = new ConcurrentHashMap<>();
    private final AtomicInteger nextProductId = new AtomicInteger(1000);
    private final AtomicInteger nextOrderId = new AtomicInteger(1000);

    public StoreService() {
        products.put(10, new ProductResponse(10, "XYZ Phone", ProductType.gadget, 10, "2023-10-01"));
        products.put(20, new ProductResponse(20, "ABC Book", ProductType.book, 5, "2023-10-01"));
        orders.put(10, new OrderResponse(10, 10, 2, OrderStatus.pending));
        orders.put(20, new OrderResponse(20, 10, 1, OrderStatus.pending));
    }

    public ProductResponse getProduct(int id) {
        ProductResponse product = products.get(id);
        if (product == null) {
            throw new NotFoundException("Product not found");
        }
        return product;
    }

    public List<ProductResponse> getProducts(ProductType type) {
        return products.values().stream()
                .filter(product -> type == null || product.type() == type)
                .sorted(Comparator.comparing(ProductResponse::id))
                .toList();
    }

    public IdResponse createProduct(ProductRequest request) {
        int id = nextProductId.getAndIncrement();
        products.put(id, new ProductResponse(id, request.name(), request.type(), request.inventory(), LocalDate.now().toString()));
        return new IdResponse(id);
    }

    public void updateProduct(int id, ProductRequest request) {
        getProduct(id);
        products.put(id, new ProductResponse(id, request.name(), request.type(), request.inventory(), LocalDate.now().toString()));
    }

    public void deleteProduct(int id) {
        if (products.remove(id) == null) {
            throw new NotFoundException("Product not found");
        }
    }

    public void updateProductImage(int id) {
        getProduct(id);
    }

    public IdResponse createOrder(OrderRequest request) {
        ProductResponse product = getProduct(request.productid());
        if (product.inventory() < request.count()) {
            throw new UnprocessableRequestException("Insufficient inventory");
        }
        products.put(product.id(), new ProductResponse(product.id(), product.name(), product.type(),
                product.inventory() - request.count(), product.createdOn()));
        int id = nextOrderId.getAndIncrement();
        orders.put(id, new OrderResponse(id, request.productid(), request.count(), OrderStatus.pending));
        return new IdResponse(id);
    }

    public List<OrderResponse> getOrders() {
        return new ArrayList<>(orders.values()).stream()
                .sorted(Comparator.comparing(OrderResponse::id))
                .toList();
    }

    public OrderResponse getOrder(int id) {
        OrderResponse order = orders.get(id);
        if (order == null) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    public void updateOrder(int id, OrderUpdateRequest request) {
        getOrder(id);
        orders.put(id, new OrderResponse(id, request.productid(), request.count(), request.status()));
    }

    public void deleteOrder(int id) {
        OrderResponse order = getOrder(id);
        orders.put(id, new OrderResponse(id, order.productid(), order.count(), OrderStatus.cancelled));
    }
}
