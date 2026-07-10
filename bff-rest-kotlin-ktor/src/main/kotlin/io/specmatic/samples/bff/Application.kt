package io.specmatic.samples.bff

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.Properties
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val mapper: ObjectMapper = jacksonObjectMapper()
private val productTypes = setOf("book", "food", "gadget", "other")
private val orderStatuses = setOf("pending", "completed", "cancelled")

fun main() {
    val port = env("SUT_PORT", "8080").toInt()
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        jackson()
    }

    val dependencyClient = DependencyClient(
        baseUrl = env("STUB_BASE_URL", "http://localhost:8090"),
        kafkaBootstrapServers = env("KAFKA_BROKER", "localhost:9092")
    )
    val monitorIds = AtomicInteger(122)
    val monitors = ConcurrentHashMap<Int, Map<String, Any>>()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        post("/products") {
            val requestedStatus = call.request.headers["Specmatic-Response-Code"]
            val body = parseBody(call.receiveText())

            if (!validProduct(body)) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Invalid product request"))
                return@post
            }

            if (requestedStatus == "202") {
                val monitorId = monitorIds.incrementAndGet()
                monitors[monitorId] = acceptedMonitor(body)
                call.response.header(HttpHeaders.Link, "</monitor/$monitorId>;rel=related;title=monitor")
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.Accepted)
                return@post
            }

            val idempotencyKey = call.request.headers["Idempotency-Key"] ?: UUID.randomUUID().toString()
            val response = dependencyClient.postJson("/products", body, idempotencyKey)
            if (response.statusCode() == 201) {
                val id = parseBody(response.body()).path("id").takeIf { it.isInt }?.intValue() ?: 1
                dependencyClient.publishProductAudit(body, id)
            }
            call.respondDependency(response, HttpStatusCode.Created)
        }

        get("/findAvailableProducts") {
            val requestedStatus = call.request.headers["Specmatic-Response-Code"]
            val type = call.request.queryParameters["type"]
            val pageSize = call.request.headers["pageSize"]
            val fromDate = call.request.queryParameters["from-date"]
            val toDate = call.request.queryParameters["to-date"]

            if (requestedStatus == "429") {
                call.response.header("Retry-After", "10")
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.TooManyRequests)
                return@get
            }

            if (!validProductSearch(type, pageSize, fromDate, toDate)) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Invalid product search request"))
                return@get
            }

            val query = listOfNotNull(
                type?.let { "type=${encode(it)}" },
                fromDate?.let { "from-date=${encode(it)}" },
                toDate?.let { "to-date=${encode(it)}" }
            ).joinToString("&").let { if (it.isBlank()) "" else "?$it" }

            val response = dependencyClient.get("/products$query", mapOf("pageSize" to pageSize!!))
            call.respondDependency(response, HttpStatusCode.OK)
        }

        post("/orders") {
            val requestedStatus = call.request.headers["Specmatic-Response-Code"]
            val body = parseBody(call.receiveText())

            if (!validOrder(body)) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Invalid order request"))
                return@post
            }

            if (requestedStatus == "202") {
                val monitorId = monitorIds.incrementAndGet()
                monitors[monitorId] = acceptedMonitor(body)
                call.response.header(HttpHeaders.Link, "</monitor/$monitorId>;rel=related;title=monitor")
                call.respondText("", ContentType.Text.Plain, HttpStatusCode.Accepted)
                return@post
            }

            val idempotencyKey = call.request.headers["Idempotency-Key"] ?: UUID.randomUUID().toString()
            val response = dependencyClient.postJson("/orders", body, idempotencyKey)
            call.respondDependency(response, HttpStatusCode.Created)
        }

        get("/orders") {
            val orderId = call.request.queryParameters["orderId"]
            if (orderId != null && orderId.toIntOrNull() == null) {
                val response = dependencyClient.get("/orders")
                call.respondText(normalizeOrderStatuses(response.body()), ContentType.Application.Json, HttpStatusCode.OK)
                return@get
            }

            val response = dependencyClient.get("/orders")
            call.respondText(normalizeOrderStatuses(response.body()), ContentType.Application.Json, HttpStatusCode.OK)
        }

        get("/monitor/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Invalid monitor id"))
                return@get
            }

            call.respond(
                monitors[id] ?: monitorForLookup(id)
            )
        }
    }
}

private class DependencyClient(
    private val baseUrl: String,
    private val kafkaBootstrapServers: String
) {
    private val httpClient = HttpClient.newBuilder().build()

    fun get(pathAndQuery: String, headers: Map<String, String> = emptyMap()): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create("$baseUrl$pathAndQuery")).GET()
        headers.forEach { (name, value) -> builder.header(name, value) }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    fun postJson(path: String, body: JsonNode, idempotencyKey: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl$path"))
            .header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            .header("Accept", ContentType.Application.Json.toString())
            .header("Authenticate", "sample-api-key")
            .header("Idempotency-Key", idempotencyKey)
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun publishProductAudit(product: JsonNode, id: Int) {
        val properties = Properties().apply {
            put("bootstrap.servers", kafkaBootstrapServers)
            put("key.serializer", StringSerializer::class.java.name)
            put("value.serializer", StringSerializer::class.java.name)
            put("acks", "all")
            put("retries", "0")
            put("max.block.ms", "3000")
            put("request.timeout.ms", "3000")
        }

        KafkaProducer<String, String>(properties).use { producer ->
            val audit = mapper.createObjectNode()
                .put("name", product.path("name").asText())
                .put("inventory", product.path("inventory").asInt())
                .put("id", id)
            audit.set<JsonNode>("categories", mapper.createArrayNode().add(category(1, product.path("type").asText())))
            producer.send(ProducerRecord("product-queries", id.toString(), mapper.writeValueAsString(audit))).get()
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondDependency(
    response: HttpResponse<String>,
    expectedSuccess: HttpStatusCode
) {
    val status = HttpStatusCode.fromValue(response.statusCode())
    val body = response.body()
    val contentType = response.headers().firstValue(HttpHeaders.ContentType).orElse(ContentType.Application.Json.toString())
    if (body.isBlank()) {
        respondText("", ContentType.parse(contentType), status)
    } else {
        respondText(body, ContentType.parse(contentType), status.takeIf { it.value != 0 } ?: expectedSuccess)
    }
}

private fun validProduct(body: JsonNode): Boolean =
    body.isObject &&
        body.path("name").isTextual &&
        body.path("type").asText() in productTypes &&
        body.path("inventory").isInt &&
        body.path("inventory").intValue() in 1..101

private fun validOrder(body: JsonNode): Boolean =
    body.isObject &&
        body.path("productid").isInt &&
        body.path("count").isInt

private fun validProductSearch(type: String?, pageSize: String?, fromDate: String?, toDate: String?): Boolean {
    if (type != null && type !in productTypes) return false
    if (pageSize?.toIntOrNull() == null) return false
    if (fromDate == null || toDate == null) return false
    return runCatching { LocalDate.parse(fromDate); LocalDate.parse(toDate) }.isSuccess
}

private fun parseBody(text: String): JsonNode =
    runCatching { mapper.readTree(text) }.getOrElse { mapper.createObjectNode() }

private fun badRequest(message: String): ObjectNode =
    mapper.createObjectNode()
        .put("timestamp", Instant.now().toString())
        .put("status", 400)
        .put("error", "Bad Request")
        .put("message", message)

private fun category(id: Int, name: String): ObjectNode =
    mapper.createObjectNode().put("id", id).put("name", name)

private fun acceptedMonitor(requestBody: JsonNode): Map<String, Any> =
    mapOf(
        "request" to mapOf(
            "method" to "POST",
            "body" to mapper.convertValue(requestBody, Map::class.java),
            "headers" to listOf(mapOf("name" to "Content-Type", "value" to "application/json"))
        ),
        "response" to mapOf(
            "statusCode" to 201,
            "body" to mapOf("id" to 123),
            "headers" to listOf(mapOf("name" to "Content-Type", "value" to "application/json"))
        )
    )

private fun monitorForLookup(id: Int): Map<String, Any> =
    mapOf(
        "request" to mapOf(
            "method" to "GET",
            "body" to mapOf("id" to id),
            "headers" to listOf(mapOf("name" to "Accept", "value" to "application/json"))
        ),
        "response" to mapOf(
            "statusCode" to 201,
            "body" to mapOf("id" to id),
            "headers" to listOf(mapOf("name" to "Content-Type", "value" to "application/json"))
        )
    )

private fun normalizeOrderStatuses(body: String): String {
    val json = parseBody(body)
    if (!json.isArray) return body
    json.forEach { order ->
        if (order is ObjectNode && order.path("status").asText() == "fulfilled") {
            order.put("status", "completed")
        }
    }
    return mapper.writeValueAsString(json)
}

private fun env(name: String, default: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

private fun encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)
