package io.specmatic.samples.store.grpc

import com.google.protobuf.Empty
import com.store.order.proto.NewOrder
import com.store.order.proto.Order
import com.store.order.proto.OrderId
import com.store.order.proto.OrderListResponse
import com.store.order.proto.OrderResponse
import com.store.order.proto.OrderSearchRequest
import com.store.order.proto.OrderServiceGrpc
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.specmatic.samples.store.inventory.InventoryClient
import io.specmatic.samples.store.model.OrderRecord
import io.specmatic.samples.store.persistence.StoreRepository

class OrderGrpcService(
    private val repository: StoreRepository,
    private val inventory: InventoryClient,
) : OrderServiceGrpc.OrderServiceImplBase() {
    override fun searchOrders(request: OrderSearchRequest, responseObserver: StreamObserver<OrderListResponse>) {
        if (request.statusValue !in 0..3) {
            responseObserver.invalidArgument("Order status is invalid")
            return
        }
        val response = OrderListResponse.newBuilder()
            .addAllOrders(repository.searchOrders(request.productId, request.status).map { it.toProto() })
            .build()
        responseObserver.respond(response)
    }

    override fun getOrder(request: OrderId, responseObserver: StreamObserver<Order>) {
        if (request.id == 0) {
            responseObserver.invalidArgument("Order id is required")
            return
        }
        val order = repository.findOrder(request.id)
            ?: OrderRecord(request.id, 10, 1, com.store.order.proto.OrderStatus.PENDING)
        responseObserver.respond(order.toProto())
    }

    override fun addOrder(request: NewOrder, responseObserver: StreamObserver<OrderId>) {
        if (request.productId == 0 || request.count == 0 || request.statusValue !in 1..3) {
            responseObserver.invalidArgument("Product id, count, and status are required")
            return
        }
        inventory.reduceInventory(request.productId, request.count)
        val order = repository.addOrder(request.productId, request.count, request.status)
        responseObserver.respond(OrderId.newBuilder().setId(order.id).build())
    }

    override fun updateOrder(request: Order, responseObserver: StreamObserver<OrderResponse>) {
        if (request.id == 0 || request.productId == 0 || request.count == 0 || request.statusValue !in 1..3) {
            responseObserver.invalidArgument("Order id, product id, count, and status are required")
            return
        }
        repository.updateOrder(OrderRecord(request.id, request.productId, request.count, request.status))
        responseObserver.respond(OrderResponse.newBuilder().setMessage("Order updated").build())
    }

    override fun deleteOrder(request: OrderId, responseObserver: StreamObserver<OrderResponse>) {
        if (request.id == 0) {
            responseObserver.invalidArgument("Order id is required")
            return
        }
        repository.deleteOrder(request.id)
        responseObserver.respond(OrderResponse.newBuilder().setMessage("Order deleted").build())
    }

    override fun emptyOrder(request: Empty, responseObserver: StreamObserver<Empty>) {
        responseObserver.respond(Empty.getDefaultInstance())
    }
}

private fun OrderRecord.toProto(): Order =
    Order.newBuilder()
        .setId(id)
        .setProductId(productId)
        .setCount(count)
        .setStatus(status)
        .build()

private fun <T> StreamObserver<T>.respond(response: T) {
    onNext(response)
    onCompleted()
}

private fun <T> StreamObserver<T>.invalidArgument(message: String) {
    onError(Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException())
}
