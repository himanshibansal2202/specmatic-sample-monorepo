package io.specmatic.samples.store.persistence

import com.store.order.proto.OrderStatus
import com.store.product.proto.ProductType
import io.specmatic.samples.store.model.OrderRecord
import io.specmatic.samples.store.model.ProductRecord
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class StoreRepository private constructor(
    products: List<ProductRecord>,
    orders: List<OrderRecord>,
) {
    private val products = ConcurrentHashMap(products.associateBy { it.id })
    private val orders = ConcurrentHashMap(orders.associateBy { it.id })
    private val nextProductId = AtomicInteger(15)
    private val nextOrderId = AtomicInteger(21)

    fun searchProducts(type: ProductType): List<ProductRecord> =
        products.values
            .filter { type == ProductType.NULL_PROD_TYPE || it.type == type }
            .sortedBy { it.id }

    fun findProduct(id: Int): ProductRecord? = products[id]

    fun addProduct(name: String, type: ProductType, inventory: Int): ProductRecord {
        val id = nextProductId.getAndIncrement()
        val product = ProductRecord(id, name, type, inventory)
        products[id] = product
        return product
    }

    fun updateProduct(product: ProductRecord): Boolean {
        products[product.id] = product
        return true
    }

    fun deleteProduct(id: Int): Boolean = products.remove(id) != null

    fun updateInventory(productId: Int, inventory: Int) {
        products.computeIfPresent(productId) { _, product -> product.copy(inventory = inventory) }
    }

    fun searchOrders(productId: Int, status: OrderStatus): List<OrderRecord> =
        orders.values
            .filter { productId == 0 || it.productId == productId }
            .filter { status == OrderStatus.NULL_ORD_STATUS || it.status == status }
            .sortedBy { it.id }

    fun findOrder(id: Int): OrderRecord? = orders[id]

    fun addOrder(productId: Int, count: Int, status: OrderStatus): OrderRecord {
        val id = nextOrderId.getAndIncrement()
        val order = OrderRecord(id, productId, count, status)
        orders[id] = order
        return order
    }

    fun updateOrder(order: OrderRecord): Boolean {
        orders[order.id] = order
        return true
    }

    fun deleteOrder(id: Int): Boolean = orders.remove(id) != null

    companion object {
        fun seeded(): StoreRepository =
            StoreRepository(
                products = listOf(
                    ProductRecord(1, "Effective Java", ProductType.BOOK, 5),
                    ProductRecord(10, "Wireless Mouse", ProductType.GADGET, 25),
                    ProductRecord(11, "Coffee Beans", ProductType.FOOD, 40),
                ),
                orders = listOf(
                    OrderRecord(1, 10, 2, OrderStatus.PENDING),
                    OrderRecord(2, 1, 1, OrderStatus.FULFILLED),
                ),
            )
    }
}
