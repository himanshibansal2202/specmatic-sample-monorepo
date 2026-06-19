package io.specmatic.samples.store.model

import com.store.order.proto.OrderStatus
import com.store.product.proto.ProductType

data class ProductRecord(
    val id: Int,
    val name: String,
    val type: ProductType,
    val inventory: Int,
)

data class OrderRecord(
    val id: Int,
    val productId: Int,
    val count: Int,
    val status: OrderStatus,
)
