package io.specmatic.examples.store

import com.store.product.proto.NewProduct
import com.store.product.proto.Product
import com.store.product.proto.ProductId
import com.store.product.proto.ProductListResponse
import com.store.product.proto.ProductResponse
import com.store.product.proto.ProductSearchRequest
import com.store.product.proto.ProductServiceGrpcKt
import com.store.product.proto.ProductType
import io.grpc.Status

class ProductGrpcService(private val store: Store) : ProductServiceGrpcKt.ProductServiceCoroutineImplBase() {
    override suspend fun searchProducts(request: ProductSearchRequest): ProductListResponse =
        ProductListResponse.newBuilder()
            .addAllProducts(store.searchProducts(request.type))
            .build()

    override suspend fun getProduct(request: ProductId): Product {
        requirePresent(request.id, "id")
        return store.getProduct(request.id)
    }

    override suspend fun addProduct(request: NewProduct): ProductId {
        requirePresent(request.name, "name")
        requirePresent(request.type, "type")
        return ProductId.newBuilder()
            .setId(store.addProduct(request.name, request.type, request.inventory))
            .build()
    }

    override suspend fun updateProduct(request: Product): ProductResponse {
        requirePresent(request.id, "id")
        requirePresent(request.name, "name")
        requirePresent(request.type, "type")
        requirePresent(request.inventory, "inventory")
        return ProductResponse.newBuilder()
            .setMessage(store.updateProduct(request))
            .build()
    }

    override suspend fun deleteProduct(request: ProductId): ProductResponse {
        requirePresent(request.id, "id")
        return ProductResponse.newBuilder()
            .setMessage(store.deleteProduct(request.id))
            .build()
    }

    private fun requirePresent(value: Int, field: String) {
        if (value == 0) throw invalidArgument(field)
    }

    private fun requirePresent(value: String, field: String) {
        if (value.isBlank()) throw invalidArgument(field)
    }

    private fun requirePresent(value: ProductType, field: String) {
        if (value == ProductType.NULL_PROD_TYPE || value == ProductType.UNRECOGNIZED) {
            throw invalidArgument(field)
        }
    }

    private fun invalidArgument(field: String) =
        Status.INVALID_ARGUMENT
            .withDescription("Missing required field: $field")
            .asRuntimeException()
}
