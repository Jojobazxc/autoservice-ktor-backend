package com.example.features.reports

import com.example.common.BadRequestException
import com.example.common.enums.OrderStatus
import com.example.common.enums.PaymentMethod
import com.example.common.enums.PaymentStatus
import com.example.common.enums.UserRole
import com.example.features.auth.authorized
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Application.configureAccountingReportRoutes() {
    val repository = AccountingReportRepository()

    routing {
        authorized(UserRole.ADMIN, UserRole.ACCOUNTANT) {
            route("/api/reports/accounting") {
                get("/summary") {
                    val period = call.periodFilter()
                    call.respond(repository.getSummary(period.from, period.to))
                }

                get("/payments") {
                    call.respond(repository.getPayments(call.accountingPaymentsFilter()))
                }

                get("/debts") {
                    call.respond(repository.getDebts(call.accountingDebtsFilter()))
                }
            }
        }
    }
}

private data class PeriodFilter(
    val from: LocalDateTime?,
    val to: LocalDateTime?
)

private fun ApplicationCall.accountingPaymentsFilter(): AccountingPaymentsFilter {
    val period = periodFilter()

    return AccountingPaymentsFilter(
        status = parseEnumQuery<PaymentStatus>("status"),
        method = parseEnumQuery<PaymentMethod>("method"),
        clientId = parseLongQuery("clientId"),
        orderId = parseLongQuery("orderId"),
        from = period.from,
        to = period.to,
        page = parsePositiveIntQuery("page", 1),
        limit = parsePositiveIntQuery("limit", 20, max = 100),
        sortField = parseSortField(
            allowed = setOf("paymentId", "orderId", "amount", "paidAt"),
            default = "paidAt"
        ),
        sortOrder = parseSortOrder(default = SortOrder.DESC)
    )
}

private fun ApplicationCall.accountingDebtsFilter(): AccountingDebtsFilter {
    val period = periodFilter()

    return AccountingDebtsFilter(
        clientId = parseLongQuery("clientId"),
        orderStatus = parseEnumQuery<OrderStatus>("orderStatus"),
        from = period.from,
        to = period.to,
        page = parsePositiveIntQuery("page", 1),
        limit = parsePositiveIntQuery("limit", 20, max = 100),
        sortField = parseSortField(
            allowed = setOf("orderId", "totalAmount", "paidAmount", "debtAmount", "lastPaymentAt"),
            default = "debtAmount"
        ),
        sortOrder = parseSortOrder(default = SortOrder.DESC)
    )
}

private fun ApplicationCall.periodFilter(): PeriodFilter {
    val from = parseDateQuery("from")?.atStartOfDay()
    val to = parseDateQuery("to")?.atTime(LocalTime.MAX)

    if (from != null && to != null && from > to) {
        throw BadRequestException("from must be before or equal to to")
    }

    return PeriodFilter(from, to)
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

private fun ApplicationCall.parseLongQuery(name: String): Long? {
    val value = request.queryParameters[name] ?: return null
    val parsed = value.toLongOrNull()
        ?: throw BadRequestException("$name must be a valid number")

    if (parsed <= 0) {
        throw BadRequestException("$name must be greater than 0")
    }

    return parsed
}

private fun ApplicationCall.parsePositiveIntQuery(name: String, default: Int, max: Int? = null): Int {
    val value = request.queryParameters[name] ?: return default
    val parsed = value.toIntOrNull()
        ?: throw BadRequestException("$name must be a valid number")

    if (parsed <= 0) {
        throw BadRequestException("$name must be greater than 0")
    }

    if (max != null && parsed > max) {
        throw BadRequestException("$name must be less than or equal to $max")
    }

    return parsed
}

private fun ApplicationCall.parseSortField(allowed: Set<String>, default: String): String {
    val sort = request.queryParameters["sort"] ?: return default
    val field = sort.substringBefore(",")

    if (field !in allowed) {
        throw BadRequestException("sort field must be one of ${allowed.joinToString(", ")}")
    }

    return field
}

private fun ApplicationCall.parseSortOrder(default: SortOrder): SortOrder {
    val sort = request.queryParameters["sort"] ?: return default
    val direction = sort.substringAfter(",", missingDelimiterValue = default.name).uppercase()

    return when (direction) {
        "ASC" -> SortOrder.ASC
        "DESC" -> SortOrder.DESC
        else -> throw BadRequestException("sort direction must be asc or desc")
    }
}
