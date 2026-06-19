package io.specmatic.samples.store

import io.specmatic.grpc.junit.SpecmaticGrpcContractTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ContractTest : SpecmaticGrpcContractTest {
    companion object {
        private lateinit var app: StoreApplication

        @JvmStatic
        @BeforeAll
        fun startApplication() {
            val port = System.getenv("SUT_PORT")?.toIntOrNull()
                ?: System.getProperty("SUT_PORT")?.toIntOrNull()
                ?: 8080
            System.setProperty("PORT", port.toString())
            stageGrpcImports()
            app = StoreApplication(AppConfig(grpcPort = port))
            app.start()
        }

        @JvmStatic
        @AfterAll
        fun stopApplication() {
            app.stop()
        }

        private fun stageGrpcImports() {
            copyProto("src/main/proto/order_api/product_types.proto", ".specmatic_grpc_working_dir/order_api/product_types.proto")
            copyProto("src/main/proto/order_api/order_types.proto", ".specmatic_grpc_working_dir/order_api/order_types.proto")
            copyProto("src/main/proto/buf/validate/validate.proto", ".specmatic_grpc_working_dir/buf/validate/validate.proto")
        }

        private fun copyProto(source: String, target: String) {
            val sourcePath = Path.of(source)
            val targetPath = Path.of(target)
            Files.createDirectories(targetPath.parent)
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
