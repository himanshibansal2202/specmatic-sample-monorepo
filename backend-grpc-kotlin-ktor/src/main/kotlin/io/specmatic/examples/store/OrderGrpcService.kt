package io.specmatic.examples.store

import com.google.protobuf.Empty
import com.store.order.proto.NewOrder
import com.store.order.proto.Order
import com.store.order.proto.OrderId
import com.store.order.proto.OrderListResponse
import com.store.order.proto.OrderResponse
import com.store.order.proto.OrderSearchRequest
import com.store.order.proto.OrderServiceGrpcKt
import com.store.order.proto.OrderStatus
import io.grpc.Status

class OrderGrpcService(private val store: Store) : OrderServiceGrpcKt.OrderServiceCoroutineImplBase() {
    override suspend fun searchOrders(request: OrderSearchRequest): OrderListResponse =
        OrderListResponse.newBuilder()
            .addAllOrders(store.searchOrders(request.productId, request.status))
            .build()

    override suspend fun getOrder(request: OrderId): Order {
        requirePresent(request.id, "id")
        return store.getOrder(request.id)
    }

    override suspend fun addOrder(request: NewOrder): OrderId {
        requirePresent(request.productId, "productId")
        requirePresent(request.count, "count")
        requirePresent(request.status, "status")
        return OrderId.newBuilder()
            .setId(store.addOrder(request.productId, request.count, request.status))
            .build()
    }

    override suspend fun updateOrder(request: Order): OrderResponse {
        requirePresent(request.id, "id")
        requirePresent(request.productId, "productId")
        requirePresent(request.count, "count")
        requirePresent(request.status, "status")
        return OrderResponse.newBuilder()
            .setMessage(store.updateOrder(request))
            .build()
    }

    override suspend fun deleteOrder(request: OrderId): OrderResponse {
        requirePresent(request.id, "id")
        return OrderResponse.newBuilder()
            .setMessage(store.deleteOrder(request.id))
            .build()
    }

    override suspend fun emptyOrder(request: Empty): Empty = Empty.getDefaultInstance()

    private fun requirePresent(value: Int, field: String) {
        if (value == 0) throw invalidArgument(field)
    }

    private fun requirePresent(value: OrderStatus, field: String) {
        if (value == OrderStatus.NULL_ORD_STATUS || value == OrderStatus.UNRECOGNIZED) {
            throw invalidArgument(field)
        }
    }

    private fun invalidArgument(field: String) =
        Status.INVALID_ARGUMENT
            .withDescription("Missing required field: $field")
            .asRuntimeException()
}
