package io.specmatic.samples.store.inventory

interface InventoryClient {
    fun addInventory(productId: Int, count: Int)
    fun getInventory(productId: Int): Int
    fun reduceInventory(productId: Int, count: Int): Boolean
}
