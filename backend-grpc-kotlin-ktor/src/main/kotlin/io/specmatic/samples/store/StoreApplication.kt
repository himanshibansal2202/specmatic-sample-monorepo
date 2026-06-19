package io.specmatic.samples.store

import io.grpc.Server
import io.grpc.ServerBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.specmatic.samples.store.grpc.OrderGrpcService
import io.specmatic.samples.store.grpc.ProductGrpcService
import io.specmatic.samples.store.inventory.InMemoryInventoryClient
import io.specmatic.samples.store.persistence.StoreRepository

class StoreApplication(private val config: AppConfig) {
    private val repository = StoreRepository.seeded()
    private val inventory = InMemoryInventoryClient(repository)
    private var grpcServer: Server? = null
    private var managementServer: EmbeddedServer<*, *>? = null

    fun start() {
        grpcServer = ServerBuilder
            .forPort(config.grpcPort)
            .addService(ProductGrpcService(repository, inventory))
            .addService(OrderGrpcService(repository, inventory))
            .build()
            .start()

        managementServer = embeddedServer(Netty, port = config.managementPort) {
            routing {
                get("/health") {
                    call.respondText("ok", status = HttpStatusCode.OK)
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        grpcServer?.shutdownNow()
        managementServer?.stop(500, 1_000)
    }
}
