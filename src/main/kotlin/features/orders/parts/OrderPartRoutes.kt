package com.example.features.orders.parts

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.math.BigDecimal

fun Application.configureOrderPartRoutes() {
    val repository = OrderPartRepository()

    routing {
        route("/orders/{orderId}/parts") {
            get {
                val orderId = call.parameters["orderId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid order id")

                if (!repository.orderExists(orderId)) {
                    throw NotFoundException("Order not found")
                }

                call.respond(repository.getAllByOrderId(orderId))
            }

            post {
                val orderId = call.parameters["orderId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid order id")

                if (!repository.orderExists(orderId)) {
                    throw NotFoundException("Order not found")
                }

                val request = call.receive<AddOrderPartRequest>()
                validateAddOrderPartRequest(request, repository, orderId)

                val createdItem = repository.add(orderId, request)
                call.respond(HttpStatusCode.Created, createdItem)
            }

            put("/{partId}") {
                val orderId = call.parameters["orderId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid order id")

                val partId = call.parameters["partId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid part id")

                if (!repository.orderExists(orderId)) {
                    throw NotFoundException("Order not found")
                }

                if (!repository.itemExists(orderId, partId)) {
                    throw NotFoundException("Order part item not found")
                }

                val request = call.receive<UpdateOrderPartRequest>()
                validateUpdateOrderPartRequest(request)

                val updated = repository.update(orderId, partId, request)
                if (!updated) {
                    throw NotFoundException("Order part item not found")
                }

                val updatedItem = repository.getAllByOrderId(orderId)
                    .firstOrNull { it.partId == partId }
                    ?: throw NotFoundException("Order part item not found")

                call.respond(updatedItem)
            }

            delete("/{partId}") {
                val orderId = call.parameters["orderId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid order id")

                val partId = call.parameters["partId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid part id")

                if (!repository.orderExists(orderId)) {
                    throw NotFoundException("Order not found")
                }

                val deleted = repository.delete(orderId, partId)
                if (!deleted) {
                    throw NotFoundException("Order part item not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun validateAddOrderPartRequest(
    request: AddOrderPartRequest,
    repository: OrderPartRepository,
    orderId: Long
) {
    if (!repository.partExists(request.partId)) {
        throw NotFoundException("Part not found")
    }

    if (repository.itemExists(orderId, request.partId)) {
        throw BadRequestException("This part is already added to the order")
    }

    if (request.quantity < 1) {
        throw BadRequestException("quantity must be greater than or equal to 1")
    }

    request.priceAtOrder?.let {
        val value = try {
            it.toBigDecimal()
        } catch (_: Exception) {
            throw BadRequestException("priceAtOrder must be a valid decimal number")
        }

        if (value < BigDecimal.ZERO) {
            throw BadRequestException("priceAtOrder must be greater than or equal to 0")
        }
    }
}

private fun validateUpdateOrderPartRequest(request: UpdateOrderPartRequest) {
    if (request.quantity < 1) {
        throw BadRequestException("quantity must be greater than or equal to 1")
    }

    val value = try {
        request.priceAtOrder.toBigDecimal()
    } catch (_: Exception) {
        throw BadRequestException("priceAtOrder must be a valid decimal number")
    }

    if (value < BigDecimal.ZERO) {
        throw BadRequestException("priceAtOrder must be greater than or equal to 0")
    }
}