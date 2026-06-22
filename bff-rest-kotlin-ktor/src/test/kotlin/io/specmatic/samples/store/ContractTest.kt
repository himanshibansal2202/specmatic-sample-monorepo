package io.specmatic.samples.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.time.Duration
import kotlin.concurrent.thread

class ContractTest {
    @Test
    fun `runs Specmatic Enterprise contract tests through Docker`() {
        val port = envInt("SUT_PORT", 8080)
        val server = startServer(port)
        var mocks: Process? = null
        try {
            waitUntilHealthy(port)
            mocks = startDependencyMocks()
            waitUntilMockReady()
            val exitCode = runSpecmaticDocker(port)
            assertEquals(0, exitCode, "Specmatic Enterprise contract tests failed")
        } finally {
            mocks?.destroy()
            mocks?.waitFor()
            server.stop(1000, 3000)
        }
    }

    private fun startDependencyMocks(): Process {
        val image = env("SPECMATIC_DOCKER_IMAGE", "specmatic/enterprise:1.18.0")
        val workDir = File(".").canonicalFile
        val process = ProcessBuilder(
            "docker", "run", "--rm",
            "-p", "8090:8090",
            "-p", "9092:9092",
            "-v", "${workDir.absolutePath}:/usr/src/app",
            "-w", "/usr/src/app",
            "-e", "SPECMATIC_LICENSE_KEY=${env("SPECMATIC_LICENSE_KEY", "")}",
            "-e", "GIT_CONFIG_COUNT=1",
            "-e", "GIT_CONFIG_KEY_0=safe.directory",
            "-e", "GIT_CONFIG_VALUE_0=/usr/src/app/.specmatic/repos/specmatic-order-contracts",
            "-e", "STUB_BASE_URL=http://0.0.0.0:8090",
            "-e", "BROKER_HOST=localhost",
            "-e", "BROKER_PORT=9092",
            "-e", "BROKER_URL=localhost:9092",
            image,
            "mock",
            "--config", "specmatic.yaml"
        )
            .directory(workDir)
            .redirectErrorStream(true)
            .start()

        thread(name = "specmatic-mock-logs", isDaemon = true) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { println("[specmatic-mock] $it") }
            }
        }

        return process
    }

    private fun runSpecmaticDocker(port: Int): Int {
        val image = env("SPECMATIC_DOCKER_IMAGE", "specmatic/enterprise:1.18.0")
        val workDir = File(".").canonicalFile
        Files.createDirectories(workDir.toPath().resolve("build/reports/specmatic"))

        val command = mutableListOf(
            "docker", "run", "--rm",
            "--add-host", "host.docker.internal:host-gateway",
            "-v", "${workDir.absolutePath}:/usr/src/app",
            "-w", "/usr/src/app",
            "-e", "SPECMATIC_LICENSE_KEY=${env("SPECMATIC_LICENSE_KEY", "")}",
            "-e", "GIT_CONFIG_COUNT=1",
            "-e", "GIT_CONFIG_KEY_0=safe.directory",
            "-e", "GIT_CONFIG_VALUE_0=/usr/src/app/.specmatic/repos/specmatic-order-contracts",
            "-e", "SUT_BASE_URL=http://host.docker.internal:$port",
            "-e", "STUB_BASE_URL=http://host.docker.internal:8090",
            "-e", "BROKER_HOST=localhost",
            "-e", "BROKER_PORT=9092",
            "-e", "BROKER_URL=localhost:9092",
            image,
            "test",
            "--config", "specmatic.yaml"
        )

        val process = ProcessBuilder(command)
            .directory(workDir)
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach(::println)
        }

        return process.waitFor()
    }

    private fun waitUntilMockReady() {
        val deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos()
        var lastError: Exception? = null
        while (System.nanoTime() < deadline) {
            try {
                val connection = URI("http://localhost:8090/orders").toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 500
                connection.readTimeout = 500
                if (connection.responseCode in 200..499) {
                    return
                }
            } catch (error: Exception) {
                lastError = error
            }
            Thread.sleep(500)
        }
        throw IllegalStateException("Specmatic dependency mock did not become ready on port 8090", lastError)
    }

    private fun waitUntilHealthy(port: Int) {
        val deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos()
        var lastError: Exception? = null
        while (System.nanoTime() < deadline) {
            try {
                val connection = URI("http://localhost:$port/health").toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 500
                connection.readTimeout = 500
                if (connection.responseCode == 200) {
                    return
                }
            } catch (error: Exception) {
                lastError = error
            }
            Thread.sleep(250)
        }
        throw IllegalStateException("Ktor app did not become healthy on port $port", lastError)
    }
}

private fun env(name: String, default: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

private fun envInt(name: String, default: Int): Int =
    env(name, default.toString()).toInt()
