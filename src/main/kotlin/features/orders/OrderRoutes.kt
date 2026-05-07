package com.example.features.orders

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import com.example.common.enums.OrderStatus
import com.example.common.enums.UserRole
import com.example.features.auth.authorized
import com.example.features.auth.currentMasterId
import com.example.features.auth.currentUserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.time.LocalDateTime

private val strictOrderJson = Json {
    ignoreUnknownKeys = false
}

fun Application.configureOrderRoutes() {
    val repository = OrderRepository()

    routing {
        authorized(UserRole.ADMIN, UserRole.MANAGER, UserRole.MECHANIC, UserRole.ACCOUNTANT) {
            route("/orders") {
                get {
                    val clientId = call.request.queryParameters["clientId"]?.toLongOrNull()
                    val statusParam = call.request.queryParameters["status"]

                    if (call.request.queryParameters["clientId"] != null && clientId == null) {
                        throw BadRequestException("clientId must be a valid number")
                    }

                    val status = statusParam?.let {
                        try {
                            enumValueOf<OrderStatus>(it)
                        } catch (_: Exception) {
                            throw BadRequestException("status must be a valid OrderStatus")
                        }
                    }

                    call.respond(repository.getAll(clientId, status))
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw BadRequestException("Invalid order id")

                    val order = repository.getById(id)
                        ?: throw NotFoundException("Order not found")

                    call.respond(order)
                }
            }
        }

        authorized(UserRole.ADMIN, UserRole.MANAGER) {
            route("/orders") {
                post {
                    val request = call.receiveStrictOrderRequest<CreateOrderRequest>()
                    validateCreateOrderRequest(request, repository)

                    val createdOrder = repository.create(request)
                    call.respond(HttpStatusCode.Created, createdOrder)
                }

                put("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw BadRequestException("Invalid order id")

                    val request = call.receiveStrictOrderRequest<UpdateOrderRequest>()
                    validateUpdateOrderRequest(request, repository)

                    val updated = repository.update(id, request)
                    if (!updated) {
                        throw NotFoundException("Order not found")
                    }

                    val order = repository.getById(id)
                        ?: throw NotFoundException("Order not found")

                    call.respond(order)
                }

                post("/{id}/cancel") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw BadRequestException("Invalid order id")

                    val order = repository.cancel(id)
                        ?: throw NotFoundException("Order not found")

                    call.respond(order)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw BadRequestException("Invalid order id")

                    val deleted = repository.delete(id)
                    if (!deleted) {
                        throw NotFoundException("Order not found")
                    }

                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        authorized(UserRole.ADMIN, UserRole.MANAGER, UserRole.MECHANIC) {
            route("/orders") {
                post("/{id}/complete") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw BadRequestException("Invalid order id")
                    val assignedMasterId = if (call.currentUserRole() == UserRole.MECHANIC) {
                        call.currentMasterId()
                            ?: throw BadRequestException("Mechanic user is not linked to master")
                    } else {
                        null
                    }

                    val order = repository.complete(id, assignedMasterId)
                        ?: throw NotFoundException("Order not found")

                    call.respond(order)
                }
            }
        }
    }
}

private fun validateCreateOrderRequest(
    request: CreateOrderRequest,
    repository: OrderRepository
) {
    if (!repository.clientExists(request.clientId)) {
        throw NotFoundException("Client not found")
    }

    if (!repository.carExists(request.carId)) {
        throw NotFoundException("Car not found")
    }

    if (!repository.carBelongsToClient(request.carId, request.clientId)) {
        throw BadRequestException("Car does not belong to the specified client")
    }

    if (request.masterId != null && !repository.masterExists(request.masterId)) {
        throw NotFoundException("Master not found")
    }

    request.plannedCompletionAt?.let {
        validateDateTime(it, "plannedCompletionAt")
    }
}

private fun validateUpdateOrderRequest(
    request: UpdateOrderRequest,
    repository: OrderRepository
) {
    if (!repository.clientExists(request.clientId)) {
        throw NotFoundException("Client not found")
    }

    if (!repository.carExists(request.carId)) {
        throw NotFoundException("Car not found")
    }

    if (!repository.carBelongsToClient(request.carId, request.clientId)) {
        throw BadRequestException("Car does not belong to the specified client")
    }

    if (request.masterId != null && !repository.masterExists(request.masterId)) {
        throw NotFoundException("Master not found")
    }

    request.plannedCompletionAt?.let {
        validateDateTime(it, "plannedCompletionAt")
    }
}

private fun validateDateTime(value: String, fieldName: String) {
    try {
        LocalDateTime.parse(value)
    } catch (_: Exception) {
        throw BadRequestException("$fieldName must be a valid ISO-8601 datetime")
    }
}

private suspend inline fun <reified T> io.ktor.server.application.ApplicationCall.receiveStrictOrderRequest(): T {
    val rawBody = receiveText()
    val jsonObject = runCatching { strictOrderJson.parseToJsonElement(rawBody).jsonObject }
        .getOrElse { throw BadRequestException("Request body must be a valid JSON object") }

    val forbiddenFields = listOf("status", "completedAt").filter { it in jsonObject }
    if (forbiddenFields.isNotEmpty()) {
        throw BadRequestException("${forbiddenFields.joinToString(", ")} cannot be provided")
    }

    return runCatching { strictOrderJson.decodeFromString<T>(rawBody) }
        .getOrElse { throw BadRequestException("Request body contains invalid or unsupported fields") }
}
