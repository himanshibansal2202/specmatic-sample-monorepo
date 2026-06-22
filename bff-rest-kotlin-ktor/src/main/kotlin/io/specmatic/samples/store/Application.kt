package io.specmatic.samples.store

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val mapper: ObjectMapper = jacksonObjectMapper()
@Volatile
private var latestMonitorResponse: Map<String, Any> = monitorResponse("POST", emptyMap<String, Any>(), 202, emptyMap<String, Any>())

fun main() {
    val port = envInt("SUT_PORT", 8080)
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun startServer(port: Int = envInt("SUT_PORT", 8080)) =
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = false)

fun Application.module() {
    install(ContentNegotiation) {
        jackson()
    }

    val backend = BackendClient(env("STUB_BASE_URL", "http://localhost:8090"))

    routing {
        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }

        get("/openapi.yaml") {
            call.respondText(openApiDocument(), ContentType.parse("application/yaml"))
        }

        post("/products") {
            val body = parseBody(call.receiveText())
            if (!isValidProduct(body)) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Missing required product fields"))
                return@post
            }
            if (call.request.headers["Specmatic-Response-Code"] == "202") {
                latestMonitorResponse = monitorResponse("POST", jsonObjectToMap(body), 201, mapOf("id" to 123))
                call.response.header("Link", "</monitor/123>;rel=related;title=monitor")
                call.respondText("", status = HttpStatusCode.Accepted)
                return@post
            }

            val response = backend.createProduct(body)
            call.respondDependency(response, HttpStatusCode.Created)
        }

        get("/findAvailableProducts") {
            val pageSize = call.request.headers["pageSize"]
            val fromDate = call.request.queryParameters["from-date"]
            val toDate = call.request.queryParameters["to-date"]
            if (!isValidSearch(call.request.queryParameters["type"], pageSize, fromDate, toDate)) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Missing required search parameters"))
                return@get
            }

            val response = backend.findProducts(
                type = call.request.queryParameters["type"],
                pageSize = pageSize!!,
                fromDate = fromDate!!,
                toDate = toDate!!
            )
            call.respondDependency(response, HttpStatusCode.OK)
        }

        post("/orders") {
            val body = parseBody(call.receiveText())
            if (!isValidOrder(body)) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Missing required order fields"))
                return@post
            }
            if (call.request.headers["Specmatic-Response-Code"] == "202") {
                latestMonitorResponse = monitorResponse("POST", jsonObjectToMap(body), 201, mapOf("id" to 123))
                call.response.header("Link", "</monitor/123>;rel=related;title=monitor")
                call.respondText("", status = HttpStatusCode.Accepted)
                return@post
            }

            val response = backend.createOrder(body)
            call.respondDependency(response, HttpStatusCode.Created)
        }

        get("/orders") {
            val response = backend.getOrders()
            call.respondOrders(response)
        }

        get("/monitor/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Invalid monitor id"))
                return@get
            }

            call.respond(latestMonitorResponse)
        }
    }
}

private class BackendClient(private val baseUrl: String) {
    private val client = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            jackson()
        }
    }

    suspend fun createProduct(body: JsonNode): HttpResponse =
        client.post("$baseUrl/products") {
            contentType(ContentType.Application.Json)
            header("Authenticate", env("BACKEND_AUTHENTICATE", "sample-api-key"))
            header("Idempotency-Key", UUID.randomUUID().toString())
            setBody(body)
        }

    suspend fun findProducts(type: String?, pageSize: String, fromDate: String, toDate: String): HttpResponse {
        val query = listOfNotNull(
            type?.let { "type=$it" },
            "from-date=$fromDate",
            "to-date=$toDate"
        ).joinToString("&")
        return client.get("$baseUrl/products?$query") {
            header("pageSize", pageSize)
        }
    }

    suspend fun createOrder(body: JsonNode): HttpResponse =
        client.post("$baseUrl/orders") {
            contentType(ContentType.Application.Json)
            header("Authenticate", env("BACKEND_AUTHENTICATE", "sample-api-key"))
            header("Idempotency-Key", UUID.randomUUID().toString())
            setBody(body)
        }

    suspend fun getOrders(): HttpResponse =
        client.get("$baseUrl/orders")
}

private suspend fun io.ktor.server.application.ApplicationCall.respondDependency(
    response: HttpResponse,
    expectedSuccess: HttpStatusCode
) {
    if (response.status == expectedSuccess || response.status.value in 400..499) {
        val text = response.body<String>()
        val contentType = response.headers[HttpHeaders.ContentType] ?: ContentType.Application.Json.toString()
        respondText(text, ContentType.parse(contentType), response.status)
    } else {
        respond(response.status, badRequest("Dependency returned ${response.status.value}"))
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondOrders(response: HttpResponse) {
    if (response.status != HttpStatusCode.OK) {
        respondDependency(response, HttpStatusCode.OK)
        return
    }

    val body = mapper.readTree(response.body<String>())
    if (body.isArray) {
        body.forEach { order ->
            if (order is ObjectNode && order["status"]?.asText() == "fulfilled") {
                order.put("status", "completed")
            }
        }
    }
    respondText(mapper.writeValueAsString(body), ContentType.Application.Json, HttpStatusCode.OK)
}

private fun parseBody(text: String): JsonNode =
    try {
        mapper.readTree(text)
    } catch (error: Exception) {
        throw BadRequestException("Invalid JSON", error)
    }

@Suppress("UNCHECKED_CAST")
private fun jsonObjectToMap(node: JsonNode): Map<String, Any> =
    mapper.convertValue(node, Map::class.java) as Map<String, Any>

private fun hasRequired(node: JsonNode, vararg fields: String): Boolean =
    node is ObjectNode && fields.all { node.hasNonNull(it) }

private fun isValidProduct(node: JsonNode): Boolean =
    hasRequired(node, "name", "type", "inventory") &&
        node["name"].isTextual &&
        node["type"].isTextual &&
        node["type"].asText() in setOf("book", "food", "gadget", "other") &&
        node["inventory"].isInt &&
        node["inventory"].asInt() in 1..101

private fun isValidOrder(node: JsonNode): Boolean =
    hasRequired(node, "productid", "count") &&
        node["productid"].isInt &&
        node["count"].isInt

private fun isValidSearch(type: String?, pageSize: String?, fromDate: String?, toDate: String?): Boolean =
    (type == null || type in setOf("book", "food", "gadget", "other")) &&
        pageSize?.toIntOrNull() != null &&
        fromDate?.let(::isDate) == true &&
        toDate?.let(::isDate) == true

private fun isDate(value: String): Boolean =
    try {
        LocalDate.parse(value)
        true
    } catch (_: Exception) {
        false
    }

private fun badRequest(message: String): Map<String, Any> =
    mapOf(
        "timestamp" to Instant.now().toString(),
        "status" to 400,
        "error" to "Bad Request",
        "message" to message
    )

private fun monitorResponse(method: String, requestBody: Map<String, Any>, statusCode: Int, responseBody: Map<String, Any>): Map<String, Any> =
    mapOf(
        "request" to mapOf(
            "method" to method,
            "body" to requestBody,
            "headers" to listOf(mapOf("name" to "Content-Type", "value" to "application/json"))
        ),
        "response" to mapOf(
            "statusCode" to statusCode,
            "body" to responseBody,
            "headers" to listOf(mapOf("name" to "Content-Type", "value" to "application/json"))
        )
    )

private fun env(name: String, default: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

private fun envInt(name: String, default: Int): Int =
    env(name, default.toString()).toInt()

private fun openApiDocument(): String = """
openapi: 3.0.0
info:
  title: Order BFF
  version: '6.0'
paths:
  /products:
    post:
      responses:
        '201':
          description: Product created
        '400':
          description: Bad Request
  /findAvailableProducts:
    get:
      responses:
        '200':
          description: OK
        '400':
          description: Bad Request
  /orders:
    post:
      responses:
        '201':
          description: Order created
        '400':
          description: Bad Request
    get:
      responses:
        '200':
          description: Successful response
  /monitor/{id}:
    get:
      responses:
        '200':
          description: Monitor status
        '400':
          description: Bad Request
""".trimIndent()
