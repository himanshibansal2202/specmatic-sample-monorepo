package io.specmatic.sample

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

class SpecmaticDockerCliTest {
  @Test
  fun `contract tests pass with Specmatic Enterprise Docker CLI`() {
    val port = env("SUT_PORT", "8080").toInt()
    val kafkaContainer = env("KAFKA_CONTAINER_NAME", "specmatic-sample-kafka")
    val server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
      module(NoopProductAuditPublisher())
    }

    try {
      startKafka(kafkaContainer)
      server.start(wait = false)
      waitForServer(port)
      val exit = runSpecmatic()
      assertEquals(0, exit, "Specmatic Enterprise Docker CLI reported contract test failures")
    } finally {
      server.stop(1000, 3000)
      stopContainer(kafkaContainer)
    }
  }

  private fun runSpecmatic(): Int {
    val projectDir = File(".").canonicalFile
    val image = env("SPECMATIC_DOCKER_IMAGE", "specmatic/enterprise:1.19.1")
    val hostUrl = env("SUT_BASE_URL", "http://host.docker.internal:8080")
    val kafkaBroker = env("KAFKA_BROKER", "host.docker.internal:9092")
    val reportDir = env("SPECMATIC_REPORT_DIR", "build/reports/specmatic")
    File(projectDir, reportDir).mkdirs()

    val command = mutableListOf(
      "docker", "run", "--rm",
      "--add-host=host.docker.internal:host-gateway",
      "-e", "SPECMATIC_LICENSE_KEY=${env("SPECMATIC_LICENSE_KEY", "")}",
      "-e", "SUT_BASE_URL=$hostUrl",
      "-e", "KAFKA_BROKER=$kafkaBroker",
      "-v", "${projectDir.absolutePath}:/workspace",
      "-w", "/workspace",
      image,
      "run-suite", "--config", "specmatic.yaml"
    )

    val process = ProcessBuilder(command)
      .directory(projectDir)
      .redirectErrorStream(true)
      .start()

    process.inputStream.bufferedReader().useLines { lines ->
      lines.forEach { println(it) }
    }

    if (!process.waitFor(5, TimeUnit.MINUTES)) {
      process.destroyForcibly()
      error("Specmatic run exceeded 5 minutes")
    }

    return process.exitValue()
  }

  private fun startKafka(containerName: String) {
    stopContainer(containerName)
    val command = listOf(
      "docker", "run", "-d",
      "--name", containerName,
      "-p", "9092:9092",
      "-e", "KAFKA_NODE_ID=1",
      "-e", "KAFKA_PROCESS_ROLES=broker,controller",
      "-e", "KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093",
      "-e", "KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093",
      "-e", "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://host.docker.internal:9092",
      "-e", "KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER",
      "-e", "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT",
      "-e", "KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT",
      "apache/kafka-native:4.0.0"
    )
    val exit = runCommand(command, inheritOutput = true)
    assertEquals(0, exit, "Kafka container failed to start")
    waitForKafka(containerName)
  }

  private fun waitForKafka(containerName: String) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
    while (System.nanoTime() < deadline) {
      try {
        java.net.Socket("localhost", 9092).use { return }
      } catch (_: Exception) {
        Thread.sleep(1000)
      }
    }
    error("Kafka did not become ready on port 9092")
  }

  private fun stopContainer(containerName: String) {
    runCommand(listOf("docker", "rm", "-f", containerName), inheritOutput = false)
  }

  private fun runCommand(command: List<String>, inheritOutput: Boolean): Int {
    val builder = ProcessBuilder(command).redirectErrorStream(true)
    val process = builder.start()
    process.inputStream.bufferedReader().useLines { lines ->
      lines.forEach { if (inheritOutput) println(it) }
    }
    process.waitFor(60, TimeUnit.SECONDS)
    return process.exitValue()
  }

  private fun waitForServer(port: Int) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
    while (System.nanoTime() < deadline) {
      try {
        java.net.URI("http://localhost:$port/health").toURL().openStream().use { return }
      } catch (_: Exception) {
        Thread.sleep(250)
      }
    }
    error("Ktor app did not start on port $port")
  }

  private fun env(name: String, defaultValue: String): String = System.getenv(name) ?: defaultValue
}
