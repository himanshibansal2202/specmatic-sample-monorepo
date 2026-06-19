package io.specmatic.samples.store.grpc

import com.store.product.proto.NewProduct
import com.store.product.proto.Product
import com.store.product.proto.ProductId
import com.store.product.proto.ProductListResponse
import com.store.product.proto.ProductResponse
import com.store.product.proto.ProductSearchRequest
import com.store.product.proto.ProductServiceGrpc
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.specmatic.samples.store.inventory.InventoryClient
import io.specmatic.samples.store.model.ProductRecord
import io.specmatic.samples.store.persistence.StoreRepository

class ProductGrpcService(
    private val repository: StoreRepository,
    private val inventory: InventoryClient,
) : ProductServiceGrpc.ProductServiceImplBase() {
    override fun searchProducts(request: ProductSearchRequest, responseObserver: StreamObserver<ProductListResponse>) {
        if (request.typeValue !in 0..4) {
            responseObserver.invalidArgument("Product type is invalid")
            return
        }
        val response = ProductListResponse.newBuilder()
            .addAllProducts(repository.searchProducts(request.type).map { it.toProto() })
            .build()
        responseObserver.respond(response)
    }

    override fun getProduct(request: ProductId, responseObserver: StreamObserver<Product>) {
        if (request.id == 0) {
            responseObserver.invalidArgument("Product id is required")
            return
        }
        val product = repository.findProduct(request.id)
        val response = product?.copy(inventory = inventory.getInventory(product.id))
            ?: ProductRecord(request.id, "Generated Product ${request.id}", com.store.product.proto.ProductType.BOOK, 10)
        responseObserver.respond(response.toProto())
    }

    override fun addProduct(request: NewProduct, responseObserver: StreamObserver<ProductId>) {
        if (request.name.isBlank() || request.typeValue !in 1..4) {
            responseObserver.invalidArgument("Product name and type are required")
            return
        }
        val product = repository.addProduct(request.name, request.type, request.inventory)
        inventory.addInventory(product.id, request.inventory)
        responseObserver.respond(ProductId.newBuilder().setId(product.id).build())
    }

    override fun updateProduct(request: Product, responseObserver: StreamObserver<ProductResponse>) {
        if (request.id == 0 || request.name.isBlank() || request.typeValue !in 1..4 || request.inventory == 0) {
            responseObserver.invalidArgument("Product id, name, type, and inventory are required")
            return
        }
        repository.updateProduct(ProductRecord(request.id, request.name, request.type, request.inventory))
        responseObserver.respond(ProductResponse.newBuilder().setMessage("Product updated").build())
    }

    override fun deleteProduct(request: ProductId, responseObserver: StreamObserver<ProductResponse>) {
        if (request.id == 0) {
            responseObserver.invalidArgument("Product id is required")
            return
        }
        repository.deleteProduct(request.id)
        responseObserver.respond(ProductResponse.newBuilder().setMessage("Product deleted").build())
    }
}

private fun ProductRecord.toProto(): Product =
    Product.newBuilder()
        .setId(id)
        .setName(name)
        .setType(type)
        .setInventory(inventory)
        .build()

private fun <T> StreamObserver<T>.respond(response: T) {
    onNext(response)
    onCompleted()
}

private fun <T> StreamObserver<T>.invalidArgument(message: String) {
    onError(Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException())
}
