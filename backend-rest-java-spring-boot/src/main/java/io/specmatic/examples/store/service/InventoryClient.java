package io.specmatic.examples.store.service;

public interface InventoryClient {
    void addInventory(int productId, int count);

    int getInventory(int productId);

    void reduceInventory(int productId, int count);
}
