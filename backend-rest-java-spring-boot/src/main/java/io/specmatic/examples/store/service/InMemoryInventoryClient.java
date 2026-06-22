package io.specmatic.examples.store.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryInventoryClient implements InventoryClient {
    private final Map<Integer, Integer> inventory = new ConcurrentHashMap<>();

    public InMemoryInventoryClient() {
        inventory.put(10, 10);
        inventory.put(20, 5);
        inventory.put(30, 7);
    }

    @Override
    public void addInventory(int productId, int count) {
        inventory.merge(productId, count, Integer::sum);
    }

    @Override
    public int getInventory(int productId) {
        return inventory.getOrDefault(productId, 0);
    }

    @Override
    public void reduceInventory(int productId, int count) {
        inventory.compute(productId, (id, current) -> Math.max(1, (current == null ? 1 : current) - count));
    }
}
