package io.specmatic.samples.store.inventory

import io.specmatic.samples.store.persistence.StoreRepository

class InMemoryInventoryClient(private val repository: StoreRepository) : InventoryClient {
    override fun addInventory(productId: Int, count: Int) {
        repository.updateInventory(productId, count)
    }

    override fun getInventory(productId: Int): Int =
        repository.findProduct(productId)?.inventory ?: 0

    override fun reduceInventory(productId: Int, count: Int): Boolean {
        val current = getInventory(productId)
        if (current < count) return false
        repository.updateInventory(productId, current - count)
        return true
    }
}
