package io.specmatic.sample.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryInventoryClient implements InventoryClient {
  private final Map<Integer, Integer> inventory = new ConcurrentHashMap<>();

  public InMemoryInventoryClient() {
    inventory.put(10, 10);
    inventory.put(20, 10);
  }

  @Override
  public void addInventory(int productId, int quantity) {
    inventory.put(productId, quantity);
  }

  @Override
  public int getInventory(int productId) {
    return inventory.getOrDefault(productId, 0);
  }

  @Override
  public boolean reduceInventory(int productId, int quantity) {
    int available = getInventory(productId);
    if (available < quantity) {
      return false;
    }
    inventory.put(productId, available - quantity);
    return true;
  }
}
