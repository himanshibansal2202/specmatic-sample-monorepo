package io.specmatic.sample.store;

public interface InventoryClient {
  void addInventory(int productId, int quantity);

  int getInventory(int productId);

  boolean reduceInventory(int productId, int quantity);
}
