package com.example.features.orders.payments

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import com.example.common.enums.PaymentMethod
import com.example.common.enums.PaymentStatus
import com.example.common.enums.UserRole
import com.example.features.auth.authorized
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Application.configurePaymentRoutes() {
    val repository = PaymentRepository()

    routing {
        authorized(UserRole.ADMIN, UserRole.ACCOUNTANT) {
            route("/payments") {
                get {
                    val filter = call.paymentFilter()
                    call.respond(repository.getAll(filter.status, filter.method, filter.from, filter.to))
                }

                get("/report") {
                    val filter = call.paymentFilter()
                    call.respond(repository.getReport(filter.status, filter.method, filter.from, filter.to))
                }

                post("/{paymentId}/pay") {
                    val paymentId = call.parameters["paymentId"]?.toLongOrNull()
                        ?: throw BadRequestException("Invalid payment id")

                    val payment = repository.pay(paymentId)
                        ?: throw NotFoundException("Payment not found")

                    call.respond(payment)
                }
            }

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
}

private data class PaymentFilter(
    val status: PaymentStatus?,
    val method: PaymentMethod?,
    val from: LocalDateTime?,
    val to: LocalDateTime?
)

private fun ApplicationCall.paymentFilter(): PaymentFilter {
    val status = parseEnumQuery<PaymentStatus>("status")
    val method = parseEnumQuery<PaymentMethod>("method")
    val from = parseDateQuery("from")?.atStartOfDay()
    val to = parseDateQuery("to")?.atTime(LocalTime.MAX)

    if (from != null && to != null && from > to) {
        throw BadRequestException("from must be before or equal to to")
    }

    return PaymentFilter(status, method, from, to)
}

private inline fun <reified T : Enum<T>> ApplicationCall.parseEnumQuery(name: String): T? {
    val value = request.queryParameters[name] ?: return null
    return runCatching { enumValueOf<T>(value) }
        .getOrElse { throw BadRequestException("$name must be a valid ${T::class.simpleName}") }
}

private fun ApplicationCall.parseDateQuery(name: String): LocalDate? {
    val value = request.queryParameters[name] ?: return null
    return runCatching { LocalDate.parse(value) }
        .getOrElse { throw BadRequestException("$name must be a valid ISO-8601 date") }
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
