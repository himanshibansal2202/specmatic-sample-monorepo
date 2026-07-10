package io.specmatic.sample

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
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
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Instant
import java.time.LocalDate
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val mapper: ObjectMapper = jacksonObjectMapper()
  .setSerializationInclusion(JsonInclude.Include.NON_NULL)
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

data class ProductBase(val name: String? = null, val type: String? = null, val inventory: Int? = null)
data class Product(val id: Int, val name: String, val type: String, val inventory: Int, val createdOn: String)
data class OrderBase(val productid: Int? = null, val count: Int? = null)
data class Order(val id: Int, val productid: Int, val count: Int, val status: String)
data class IdResponse(val id: Int)
data class HeaderItem(val name: String, val value: String)
data class MonitorRequest(val method: String, val body: Map<String, Any?>, val headers: List<HeaderItem>)
data class MonitorResult(val statusCode: Int, val body: Map<String, Any?>, val headers: List<HeaderItem>)
data class MonitorResponse(val request: MonitorRequest, val response: MonitorResult)
data class BadRequestBody(val timestamp: String, val status: Int, val error: String, val message: String)

interface ProductAuditPublisher {
  fun publish(product: Product)
}

class KafkaProductAuditPublisher(
  broker: String = env("KAFKA_BROKER", "localhost:9092"),
  private val topic: String = env("KAFKA_TOPIC", "product-queries")
) : ProductAuditPublisher {
  private val producer = KafkaProducer<String, String>(Properties().apply {
    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker)
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    put(ProducerConfig.MAX_BLOCK_MS_CONFIG, env("KAFKA_MAX_BLOCK_MS", "2000"))
    put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, env("KAFKA_REQUEST_TIMEOUT_MS", "2000"))
    put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, env("KAFKA_DELIVERY_TIMEOUT_MS", "3000"))
    put(ProducerConfig.RETRIES_CONFIG, "0")
  })

  override fun publish(product: Product) {
    val payload = mapper.writeValueAsString(
      mapOf(
        "name" to product.name,
        "inventory" to product.inventory,
        "id" to product.id,
        "categories" to listOf(mapOf("id" to 1, "name" to product.type))
      )
    )
    producer.send(ProducerRecord(topic, product.id.toString(), payload)).get()
  }
}

class NoopProductAuditPublisher : ProductAuditPublisher {
  override fun publish(product: Product) = Unit
}

fun main() {
  val port = env("SUT_PORT", "8080").toInt()
  embeddedServer(Netty, port = port, host = env("SUT_HOST", "0.0.0.0")) {
    module()
  }.start(wait = true)
}

fun Application.module(publisher: ProductAuditPublisher = configuredPublisher()) {
  val monitorIds = AtomicInteger(100)
  val monitors = ConcurrentHashMap<Int, MonitorResponse>()

  install(ContentNegotiation) {
    jackson {
      setSerializationInclusion(JsonInclude.Include.NON_NULL)
      configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
  }

  routing {
    get("/health") {
      call.respondText("OK")
    }

    post("/products") {
      val body = call.receiveJsonObject()
        ?: return@post call.respond(HttpStatusCode.BadRequest, badRequest("Invalid product request"))
      val productRequest = ProductBase(
        name = body.text("name"),
        type = body.text("type"),
        inventory = body.int("inventory")
      )
      if (!validProduct(productRequest)) return@post call.respond(HttpStatusCode.BadRequest, badRequest("Invalid product request"))

      val product = Product(1, productRequest.name!!, productRequest.type!!, productRequest.inventory!!, "2024-01-01")
      publisher.publish(product)

      when (call.request.headers["Specmatic-Response-Code"]) {
        "202" -> {
          val monitorId = monitorIds.incrementAndGet()
          monitors[monitorId] = MonitorResponse(
            request = MonitorRequest(
              method = "POST",
              body = mapOf(
                "name" to productRequest.name,
                "type" to productRequest.type,
                "inventory" to productRequest.inventory
              ),
              headers = listOf(HeaderItem("Content-Type", "application/json"))
            ),
            response = MonitorResult(
              statusCode = 201,
              body = mapOf("id" to product.id),
              headers = listOf(HeaderItem("Content-Type", "application/json"))
            )
          )
          call.response.header(HttpHeaders.Link, "</monitor/$monitorId>;rel=related;title=monitor")
          call.respond(HttpStatusCode.Accepted)
        }
        else -> call.respond(HttpStatusCode.Created, IdResponse(product.id))
      }
    }

    get("/findAvailableProducts") {
      val validationError = validateFindAvailableProducts(call.request.queryParameters, call.request.headers["pageSize"])
      if (validationError != null) return@get call.respond(HttpStatusCode.BadRequest, badRequest(validationError))

      if (call.request.headers["Specmatic-Response-Code"] == "429") {
        call.response.header("Retry-After", "30")
        return@get call.respond(HttpStatusCode.TooManyRequests)
      }

      call.respond(listOf(Product(1, "iPhone", call.request.queryParameters["type"] ?: "gadget", 100, "2024-01-01")))
    }

    post("/orders") {
      val body = call.receiveJsonObject()
        ?: return@post call.respond(HttpStatusCode.BadRequest, badRequest("Invalid order request"))
      val orderRequest = OrderBase(
        productid = body.int("productid"),
        count = body.int("count")
      )
      if (orderRequest.productid == null || orderRequest.count == null) {
        return@post call.respond(HttpStatusCode.BadRequest, badRequest("Invalid order request"))
      }

      when (call.request.headers["Specmatic-Response-Code"]) {
        "202" -> {
          val monitorId = monitorIds.incrementAndGet()
          monitors[monitorId] = MonitorResponse(
            request = MonitorRequest(
              method = "POST",
              body = mapOf("productid" to orderRequest.productid, "count" to orderRequest.count),
              headers = listOf(HeaderItem("Content-Type", "application/json"))
            ),
            response = MonitorResult(
              statusCode = 201,
              body = mapOf("id" to 1),
              headers = listOf(HeaderItem("Content-Type", "application/json"))
            )
          )
          call.response.header(HttpHeaders.Link, "</monitor/$monitorId>;rel=related;title=monitor")
          call.respond(HttpStatusCode.Accepted)
        }
        else -> call.respond(HttpStatusCode.Created, IdResponse(1))
      }
    }

    get("/orders") {
      call.respond(listOf(Order(1, call.request.queryParameters["orderId"]?.toIntOrNull() ?: 1, 2, "completed")))
    }

    get("/monitor/{id}") {
      val id = call.parameters["id"]?.toIntOrNull()
        ?: return@get call.respond(HttpStatusCode.BadRequest, badRequest("Invalid monitor id"))

      call.respond(monitors[id] ?:
        MonitorResponse(
          request = MonitorRequest(
            method = "GET",
            body = mapOf("id" to id),
            headers = listOf(HeaderItem("Accept", "application/json"))
          ),
          response = MonitorResult(
            statusCode = 201,
            body = mapOf("id" to 1),
            headers = listOf(HeaderItem("Content-Type", "application/json"))
          )
        )
      )
    }
  }
}

private fun configuredPublisher(): ProductAuditPublisher =
  if (env("PUBLISH_AUDIT_MESSAGES", "true").toBoolean()) KafkaProductAuditPublisher() else NoopProductAuditPublisher()

private fun validProduct(product: ProductBase): Boolean =
  product.name != null &&
    product.type in setOf("book", "food", "gadget", "other") &&
    product.inventory != null &&
    product.inventory in 1..101

private fun validateFindAvailableProducts(query: Parameters, pageSizeHeader: String?): String? {
  val type = query["type"]
  if (type != null && type !in setOf("book", "food", "gadget", "other")) return "Invalid product type"
  if (pageSizeHeader?.toIntOrNull() == null) return "Missing or invalid pageSize header"
  if (!validDate(query["from-date"])) return "Missing or invalid from-date"
  if (!validDate(query["to-date"])) return "Missing or invalid to-date"
  return null
}

private suspend fun ApplicationCall.receiveJsonObject(): JsonNode? =
  try {
    val raw = receiveText()
    if (raw.isBlank()) null else mapper.readTree(raw).takeIf { it.isObject }
  } catch (_: Exception) {
    null
  }

private fun JsonNode.text(name: String): String? = get(name)?.takeIf { it.isTextual }?.asText()

private fun JsonNode.int(name: String): Int? = get(name)?.takeIf { it.isInt }?.asInt()

private fun validDate(value: String?): Boolean =
  try {
    !value.isNullOrBlank() && LocalDate.parse(value) != null
  } catch (_: Exception) {
    false
  }

private fun badRequest(message: String) = BadRequestBody(
  timestamp = Instant.parse("2025-01-01T00:00:00Z").toString(),
  status = 400,
  error = "Bad Request",
  message = message
)

private fun env(name: String, defaultValue: String): String = System.getenv(name) ?: defaultValue
