package io.specmatic.examples.store.api;

import io.specmatic.examples.store.model.OrderBase;
import io.specmatic.examples.store.model.OrderStatus;
import io.specmatic.examples.store.model.OrderUpdate;
import io.specmatic.examples.store.model.ProductBase;
import io.specmatic.examples.store.model.ProductType;
import java.util.Map;
import java.util.Set;

final class RequestParser {
    private RequestParser() {
    }

    static ProductBase productBase(Map<String, Object> body) {
        requireOnly(body, Set.of("name", "type", "inventory"));
        String name = requiredString(body, "name");
        ProductType type = productType(requiredString(body, "type"));
        int inventory = requiredInteger(body, "inventory");
        if (inventory < 1 || inventory > 101) {
            throw new IllegalArgumentException("inventory must be between 1 and 101");
        }
        return new ProductBase(name, type, inventory);
    }

    static OrderBase orderBase(Map<String, Object> body) {
        requireOnly(body, Set.of("productid", "count"));
        return new OrderBase(requiredInteger(body, "productid"), requiredInteger(body, "count"));
    }

    static OrderUpdate orderUpdate(Map<String, Object> body) {
        requireOnly(body, Set.of("productid", "count", "status"));
        return new OrderUpdate(
                requiredInteger(body, "productid"),
                requiredInteger(body, "count"),
                orderStatus(requiredString(body, "status")));
    }

    private static void requireOnly(Map<String, Object> body, Set<String> keys) {
        if (body == null || !body.keySet().equals(keys)) {
            throw new IllegalArgumentException("Request body does not match the contract");
        }
    }

    private static String requiredString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return text;
    }

    private static int requiredInteger(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof Number number) || Math.rint(number.doubleValue()) != number.doubleValue()) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        return number.intValue();
    }

    private static ProductType productType(String value) {
        try {
            return ProductType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("type must be a valid ProductType", exception);
        }
    }

    private static OrderStatus orderStatus(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("status must be a valid OrderStatus", exception);
        }
    }
}
