package com.example.features.orders.services

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal

fun Application.configureOrderServiceRoutes() {
    val repository = OrderServiceRepository()

    routing {
        route("/orders/{orderId}/services") {
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

                val request = call.receive<AddOrderServiceRequest>()
                validateAddOrderServiceRequest(request, repository, orderId)

                val createdItem = repository.add(orderId, request)
                call.respond(HttpStatusCode.Created, createdItem)
            }

            put("/{serviceId}") {
                val orderId = call.parameters["orderId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid order id")

                val serviceId = call.parameters["serviceId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid service id")

                if (!repository.orderExists(orderId)) {
                    throw NotFoundException("Order not found")
                }

                if (!repository.itemExists(orderId, serviceId)) {
                    throw NotFoundException("Order service item not found")
                }

                val request = call.receive<UpdateOrderServiceRequest>()
                validateUpdateOrderServiceRequest(request)

                val updated = repository.update(orderId, serviceId, request)
                if (!updated) {
                    throw NotFoundException("Order service item not found")
                }

                val updatedItem = repository.getAllByOrderId(orderId)
                    .firstOrNull { it.serviceId == serviceId }
                    ?: throw NotFoundException("Order service item not found")

                call.respond(updatedItem)
            }

            delete("/{serviceId}") {
                val orderId = call.parameters["orderId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid order id")

                val serviceId = call.parameters["serviceId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid service id")

                if (!repository.orderExists(orderId)) {
                    throw NotFoundException("Order not found")
                }

                val deleted = repository.delete(orderId, serviceId)
                if (!deleted) {
                    throw NotFoundException("Order service item not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun validateAddOrderServiceRequest(
    request: AddOrderServiceRequest,
    repository: OrderServiceRepository,
    orderId: Long
) {
    if (!repository.serviceExists(request.serviceId)) {
        throw NotFoundException("Service not found")
    }

    if (repository.itemExists(orderId, request.serviceId)) {
        throw BadRequestException("This service is already added to the order")
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

private fun validateUpdateOrderServiceRequest(request: UpdateOrderServiceRequest) {
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