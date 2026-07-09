package io.specmatic.examples.store

import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.ktor.server.application.Application

fun main() {
    val port = System.getenv("SUT_PORT")?.toIntOrNull() ?: 8080
    val store = Store()
    val server = NettyServerBuilder
        .forPort(port)
        .addService(ProductGrpcService(store))
        .addService(OrderGrpcService(store))
        .build()

    server.start()
    Runtime.getRuntime().addShutdownHook(Thread { server.shutdown() })
    println("Order API gRPC server listening on port $port")
    server.awaitTermination()
}

@Suppress("unused")
fun Application.module() {
    // Ktor is retained as the selected application framework surface. The gRPC
    // transport is served by the standard grpc-kotlin Netty runtime.
}
