package com.example.features.orders.payments

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal

fun Application.configurePaymentRoutes() {
    val repository = PaymentRepository()

    routing {
        route("/orders/{orderId}/payments") {
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

                val request = call.receive<CreatePaymentRequest>()
                validateCreatePaymentRequest(request)

                val createdPayment = repository.create(orderId, request)
                call.respond(HttpStatusCode.Created, createdPayment)
            }
        }
    }
}

private fun validateCreatePaymentRequest(request: CreatePaymentRequest) {
    val amount = try {
        request.amount.toBigDecimal()
    } catch (_: Exception) {
        throw BadRequestException("amount must be a valid decimal number")
    }

    if (amount < BigDecimal.ZERO) {
        throw BadRequestException("amount must be greater than or equal to 0")
    }
}