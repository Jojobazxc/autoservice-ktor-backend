package com.example.features.reports

import com.example.common.enums.OrderStatus
import com.example.common.enums.PaymentMethod
import com.example.common.enums.PaymentStatus
import com.example.database.tables.CarsTable
import com.example.database.tables.ClientsTable
import com.example.database.tables.OrdersTable
import com.example.database.tables.PaymentsTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

class AccountingReportRepository {

    fun getSummary(from: LocalDateTime?, to: LocalDateTime?): AccountingSummaryResponse = transaction {
        val orders = OrdersTable
            .selectAll()
            .where { buildOrderPeriodFilter(from, to) }
            .toList()
        val payments = PaymentsTable
            .selectAll()
            .where { buildPaymentPeriodFilter(from, to) }
            .toList()

        val totalOrderAmount = orders.sumOf { it[OrdersTable.totalAmount] }
        val paidAmount = payments
            .filter { it[PaymentsTable.paymentStatus] == PaymentStatus.PAID }
            .sumOf { it[PaymentsTable.amount] }
        val unpaidAmount = (totalOrderAmount - paidAmount).coerceAtLeast(BigDecimal.ZERO)

        val byStatus = PaymentStatus.entries.map { status ->
            val statusPayments = payments.filter { it[PaymentsTable.paymentStatus] == status }
            AccountingPaymentStatusResponse(
                status = status,
                amount = statusPayments.sumOf { it[PaymentsTable.amount] }.toPlainString(),
                count = statusPayments.size
            )
        }

        AccountingSummaryResponse(
            period = AccountingPeriodResponse(
                from = from?.toLocalDate()?.toString(),
                to = to?.toLocalDate()?.toString()
            ),
            totals = AccountingTotalsResponse(
                revenue = paidAmount.toPlainString(),
                paid = paidAmount.toPlainString(),
                unpaid = unpaidAmount.toPlainString(),
                ordersCount = orders.size
            ),
            byStatus = byStatus
        )
    }

    fun getPayments(filter: AccountingPaymentsFilter): AccountingPaymentsPageResponse = transaction {
        val rows = PaymentsTable
            .innerJoin(OrdersTable, { PaymentsTable.orderId }, { OrdersTable.id })
            .innerJoin(ClientsTable, { OrdersTable.clientId }, { ClientsTable.id })
            .innerJoin(CarsTable, { OrdersTable.carId }, { CarsTable.id })
            .selectAll()
            .where { buildPaymentsFilter(filter) }
            .orderBy(paymentSortColumn(filter.sortField), filter.sortOrder)
            .map(::rowToPaymentItem)

        AccountingPaymentsPageResponse(
            items = rows.drop(filter.offset).take(filter.limit),
            page = filter.page,
            limit = filter.limit,
            total = rows.size,
            totals = AccountingPaymentsTotalsResponse(
                amount = rows.sumOf { it.amount.toBigDecimal() }.toPlainString(),
                count = rows.size,
                paidAmount = rows
                    .filter { it.status == PaymentStatus.PAID }
                    .sumOf { it.amount.toBigDecimal() }
                    .toPlainString(),
                pendingAmount = rows
                    .filter { it.status == PaymentStatus.PENDING }
                    .sumOf { it.amount.toBigDecimal() }
                    .toPlainString(),
                failedAmount = rows
                    .filter { it.status == PaymentStatus.FAILED }
                    .sumOf { it.amount.toBigDecimal() }
                    .toPlainString()
            )
        )
    }

    fun getDebts(filter: AccountingDebtsFilter): AccountingDebtsPageResponse = transaction {
        val paidRows = PaymentsTable
            .selectAll()
            .where { PaymentsTable.paymentStatus eq PaymentStatus.PAID }
            .toList()

        val paidByOrder = paidRows
            .groupBy { it[PaymentsTable.orderId] }
            .mapValues { (_, payments) -> payments.sumOf { it[PaymentsTable.amount] } }
        val lastPaymentByOrder = paidRows
            .groupBy { it[PaymentsTable.orderId] }
            .mapValues { (_, payments) -> payments.mapNotNull { it[PaymentsTable.paidAt] }.maxOrNull() }

        val rows = OrdersTable
            .innerJoin(ClientsTable, { OrdersTable.clientId }, { ClientsTable.id })
            .innerJoin(CarsTable, { OrdersTable.carId }, { CarsTable.id })
            .selectAll()
            .where { buildDebtsFilter(filter) }
            .mapNotNull { row ->
                val totalAmount = row[OrdersTable.totalAmount]
                val paidAmount = paidByOrder[row[OrdersTable.id]] ?: BigDecimal.ZERO
                val debtAmount = totalAmount - paidAmount

                if (debtAmount <= BigDecimal.ZERO) {
                    null
                } else {
                    rowToDebtItem(row, paidAmount, debtAmount, lastPaymentByOrder[row[OrdersTable.id]])
                }
            }
            .sortedWith(debtComparator(filter.sortField, filter.sortOrder))

        AccountingDebtsPageResponse(
            items = rows.drop(filter.offset).take(filter.limit),
            page = filter.page,
            limit = filter.limit,
            total = rows.size,
            totals = AccountingDebtsTotalsResponse(
                totalAmount = rows.sumOf { it.totalAmount.toBigDecimal() }.toPlainString(),
                paidAmount = rows.sumOf { it.paidAmount.toBigDecimal() }.toPlainString(),
                debtAmount = rows.sumOf { it.debtAmount.toBigDecimal() }.toPlainString(),
                ordersCount = rows.size
            )
        )
    }

    private fun rowToPaymentItem(row: ResultRow): AccountingPaymentItemResponse {
        return AccountingPaymentItemResponse(
            paymentId = row[PaymentsTable.id],
            orderId = row[PaymentsTable.orderId],
            clientName = row[ClientsTable.fullName],
            car = carLabel(row),
            amount = row[PaymentsTable.amount].toPlainString(),
            status = row[PaymentsTable.paymentStatus],
            method = row[PaymentsTable.paymentMethod],
            paidAt = row[PaymentsTable.paidAt]?.toString()
        )
    }

    private fun rowToDebtItem(
        row: ResultRow,
        paidAmount: BigDecimal,
        debtAmount: BigDecimal,
        lastPaymentAt: LocalDateTime?
    ): AccountingDebtItemResponse {
        return AccountingDebtItemResponse(
            orderId = row[OrdersTable.id],
            clientName = row[ClientsTable.fullName],
            car = carLabel(row),
            totalAmount = row[OrdersTable.totalAmount].toPlainString(),
            paidAmount = paidAmount.toPlainString(),
            debtAmount = debtAmount.toPlainString(),
            orderStatus = row[OrdersTable.status],
            lastPaymentAt = lastPaymentAt?.toString()
        )
    }

    private fun carLabel(row: ResultRow): String {
        return "${row[CarsTable.brand]} ${row[CarsTable.model]}"
    }

    private fun buildPaymentPeriodFilter(from: LocalDateTime?, to: LocalDateTime?): Op<Boolean> {
        var filter: Op<Boolean> = Op.TRUE

        if (from != null) {
            filter = filter and (PaymentsTable.paidAt greaterEq from)
        }

        if (to != null) {
            filter = filter and (PaymentsTable.paidAt lessEq to)
        }

        return filter
    }

    private fun buildOrderPeriodFilter(from: LocalDateTime?, to: LocalDateTime?): Op<Boolean> {
        var filter: Op<Boolean> = Op.TRUE

        if (from != null) {
            filter = filter and (OrdersTable.createdAt greaterEq from)
        }

        if (to != null) {
            filter = filter and (OrdersTable.createdAt lessEq to)
        }

        return filter
    }

    private fun buildPaymentsFilter(filter: AccountingPaymentsFilter): Op<Boolean> {
        var query: Op<Boolean> = Op.TRUE

        if (filter.status != null) {
            query = query and (PaymentsTable.paymentStatus eq filter.status)
        }

        if (filter.method != null) {
            query = query and (PaymentsTable.paymentMethod eq filter.method)
        }

        if (filter.clientId != null) {
            query = query and (OrdersTable.clientId eq filter.clientId)
        }

        if (filter.orderId != null) {
            query = query and (PaymentsTable.orderId eq filter.orderId)
        }

        if (filter.from != null) {
            query = query and (PaymentsTable.paidAt greaterEq filter.from)
        }

        if (filter.to != null) {
            query = query and (PaymentsTable.paidAt lessEq filter.to)
        }

        return query
    }

    private fun buildDebtsFilter(filter: AccountingDebtsFilter): Op<Boolean> {
        var query: Op<Boolean> = Op.TRUE

        if (filter.clientId != null) {
            query = query and (OrdersTable.clientId eq filter.clientId)
        }

        if (filter.orderStatus != null) {
            query = query and (OrdersTable.status eq filter.orderStatus)
        }

        if (filter.from != null) {
            query = query and (OrdersTable.createdAt greaterEq filter.from)
        }

        if (filter.to != null) {
            query = query and (OrdersTable.createdAt lessEq filter.to)
        }

        return query
    }

    private fun paymentSortColumn(field: String) = when (field) {
        "paymentId" -> PaymentsTable.id
        "orderId" -> PaymentsTable.orderId
        "amount" -> PaymentsTable.amount
        "paidAt" -> PaymentsTable.paidAt
        else -> PaymentsTable.id
    }

    private fun debtComparator(field: String, order: SortOrder): Comparator<AccountingDebtItemResponse> {
        val comparator = when (field) {
            "orderId" -> compareBy<AccountingDebtItemResponse> { it.orderId }
            "totalAmount" -> compareBy { it.totalAmount.toBigDecimal() }
            "paidAmount" -> compareBy { it.paidAmount.toBigDecimal() }
            "debtAmount" -> compareBy { it.debtAmount.toBigDecimal() }
            "lastPaymentAt" -> compareBy(nullsLast()) { it.lastPaymentAt }
            else -> compareBy { it.orderId }
        }

        return if (order == SortOrder.DESC) comparator.reversed() else comparator
    }
}

data class AccountingPaymentsFilter(
    val status: PaymentStatus?,
    val method: PaymentMethod?,
    val clientId: Long?,
    val orderId: Long?,
    val from: LocalDateTime?,
    val to: LocalDateTime?,
    val page: Int,
    val limit: Int,
    val sortField: String,
    val sortOrder: SortOrder
) {
    val offset: Int = (page - 1) * limit
}

data class AccountingDebtsFilter(
    val clientId: Long?,
    val orderStatus: OrderStatus?,
    val from: LocalDateTime?,
    val to: LocalDateTime?,
    val page: Int,
    val limit: Int,
    val sortField: String,
    val sortOrder: SortOrder
) {
    val offset: Int = (page - 1) * limit
}
