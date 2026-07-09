package io.specmatic.examples.store

import com.store.order.proto.Order
import com.store.order.proto.OrderStatus
import com.store.product.proto.Product
import com.store.product.proto.ProductType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Store {
    private val nextProductId = AtomicInteger(15)
    private val nextOrderId = AtomicInteger(21)
    private val products = ConcurrentHashMap<Int, Product>()
    private val orders = ConcurrentHashMap<Int, Order>()

    init {
        products[1] = product(1, "Effective Java", ProductType.BOOK, 5)
        products[10] = product(10, "Smartphone", ProductType.GADGET, 100)
        orders[1] = order(1, 10, 1, OrderStatus.FULFILLED)
    }

    fun searchProducts(type: ProductType): List<Product> =
        products.values
            .filter { type == ProductType.NULL_PROD_TYPE || it.type == type }
            .sortedBy { it.id }

    fun getProduct(id: Int): Product = products[id] ?: product(id, "Product $id", ProductType.OTHER, 0)

    fun addProduct(name: String, type: ProductType, inventory: Int): Int {
        val id = nextProductId.getAndIncrement()
        products[id] = product(id, name, type, inventory)
        return id
    }

    fun updateProduct(updated: Product): String {
        products[updated.id] = updated
        return "Product ${updated.id} updated"
    }

    fun deleteProduct(id: Int): String {
        products.remove(id)
        return "Product $id deleted"
    }

    fun searchOrders(productId: Int, status: OrderStatus): List<Order> =
        orders.values
            .filter { productId == 0 || it.productId == productId }
            .filter { status == OrderStatus.NULL_ORD_STATUS || it.status == status }
            .sortedBy { it.id }

    fun getOrder(id: Int): Order = orders[id] ?: order(id, 10, 1, OrderStatus.PENDING)

    fun addOrder(productId: Int, count: Int, status: OrderStatus): Int {
        val id = nextOrderId.getAndIncrement()
        orders[id] = order(id, productId, count, status)
        products.computeIfPresent(productId) { _, product ->
            product.toBuilder().setInventory((product.inventory - count).coerceAtLeast(0)).build()
        }
        return id
    }

    fun updateOrder(updated: Order): String {
        orders[updated.id] = updated
        return "Order ${updated.id} updated"
    }

    fun deleteOrder(id: Int): String {
        orders.remove(id)
        return "Order $id deleted"
    }

    private fun product(id: Int, name: String, type: ProductType, inventory: Int): Product =
        Product.newBuilder()
            .setId(id)
            .setName(name)
            .setType(type)
            .setInventory(inventory)
            .build()

    private fun order(id: Int, productId: Int, count: Int, status: OrderStatus): Order =
        Order.newBuilder()
            .setId(id)
            .setProductId(productId)
            .setCount(count)
            .setStatus(status)
            .build()
}
