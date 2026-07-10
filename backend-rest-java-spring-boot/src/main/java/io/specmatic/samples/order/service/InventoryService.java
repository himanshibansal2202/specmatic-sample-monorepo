package io.specmatic.samples.order.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {
    private final Map<Integer, Integer> inventory = new ConcurrentHashMap<>();

    public void addInventory(int productId, int count) { inventory.put(productId, count); }
    public int getInventory(int productId, int fallback) { return inventory.getOrDefault(productId, fallback); }
    public void reduceInventory(int productId, int count) {
        inventory.compute(productId, (ignored, current) -> Math.max(0, (current == null ? count : current) - count));
    }
}
