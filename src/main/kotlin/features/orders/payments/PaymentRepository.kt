package com.example.features.orders.payments

import com.example.common.ConflictException
import com.example.common.enums.OrderStatus
import com.example.common.enums.PaymentMethod
import com.example.common.enums.PaymentStatus
import com.example.database.tables.OrdersTable
import com.example.database.tables.PaymentsTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentRepository {

    fun getAll(
        status: PaymentStatus?,
        method: PaymentMethod?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): List<PaymentResponse> = transaction {
        PaymentsTable
            .selectAll()
            .where { buildPaymentFilter(status, method, from, to) }
            .orderBy(PaymentsTable.id)
            .map(::rowToPaymentResponse)
    }

    fun getReport(
        status: PaymentStatus?,
        method: PaymentMethod?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): PaymentReportResponse = transaction {
        val payments = PaymentsTable
            .selectAll()
            .where { buildPaymentFilter(status, method, from, to) }
            .toList()

        val totalAmount = payments.sumOf { it[PaymentsTable.amount] }
        val paidAmount = payments
            .filter { it[PaymentsTable.paymentStatus] == PaymentStatus.PAID }
            .sumOf { it[PaymentsTable.amount] }
        val pendingAmount = payments
            .filter { it[PaymentsTable.paymentStatus] == PaymentStatus.PENDING }
            .sumOf { it[PaymentsTable.amount] }
        val failedAmount = payments
            .filter { it[PaymentsTable.paymentStatus] == PaymentStatus.FAILED }
            .sumOf { it[PaymentsTable.amount] }

        PaymentReportResponse(
            totalCount = payments.size,
            totalAmount = totalAmount.toPlainString(),
            paidAmount = paidAmount.toPlainString(),
            pendingAmount = pendingAmount.toPlainString(),
            failedAmount = failedAmount.toPlainString()
        )
    }

    fun getAllByOrderId(orderId: Long): List<PaymentResponse> = transaction {
        PaymentsTable
            .selectAll()
            .where { PaymentsTable.orderId eq orderId }
            .map(::rowToPaymentResponse)
    }

    fun orderExists(orderId: Long): Boolean = transaction {
        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .any()
    }

    fun getOrderTotalAmount(orderId: Long): BigDecimal = transaction {
        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .single()[OrdersTable.totalAmount]
    }

    fun getPaidAmount(orderId: Long): BigDecimal = transaction {
        calculatePaidAmount(orderId)
    }

    private fun calculatePaidAmount(orderId: Long): BigDecimal {
        return PaymentsTable
            .selectAll()
            .where {
                (PaymentsTable.orderId eq orderId) and
                        (PaymentsTable.paymentStatus eq PaymentStatus.PAID)
            }
            .sumOf { it[PaymentsTable.amount] }
    }

    fun create(orderId: Long, request: CreatePaymentRequest): PaymentResponse = transaction {
        val order = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .single()
        val orderStatus = order[OrdersTable.status]

        if (orderStatus == OrderStatus.PAID || orderStatus == OrderStatus.CANCELED) {
            throw ConflictException("Payment cannot be created for paid or canceled order")
        }

        val amount = request.amount.toBigDecimal()
        val remainingDebt = order[OrdersTable.totalAmount] - calculatePaidAmount(orderId)
        if (amount > remainingDebt) {
            throw ConflictException("Payment amount exceeds remaining order debt")
        }

        val createdId = PaymentsTable.insert {
            it[PaymentsTable.orderId] = orderId
            it[PaymentsTable.amount] = amount
            it[paymentMethod] = request.paymentMethod
            it[paymentStatus] = PaymentStatus.PENDING
            it[paidAt] = null
        }[PaymentsTable.id]

        PaymentsTable
            .selectAll()
            .where { PaymentsTable.id eq createdId }
            .map(::rowToPaymentResponse)
            .single()
    }

    fun pay(paymentId: Long): PaymentResponse? = transaction {
        val payment = PaymentsTable
            .selectAll()
            .where { PaymentsTable.id eq paymentId }
            .singleOrNull()
            ?: return@transaction null

        if (payment[PaymentsTable.paymentStatus] != PaymentStatus.PENDING) {
            throw ConflictException("Only pending payment can be paid")
        }

        PaymentsTable.update({ PaymentsTable.id eq paymentId }) {
            it[paymentStatus] = PaymentStatus.PAID
            it[paidAt] = LocalDateTime.now()
        }
        updateOrderStatusIfNeeded(payment[PaymentsTable.orderId])

        PaymentsTable
            .selectAll()
            .where { PaymentsTable.id eq paymentId }
            .map(::rowToPaymentResponse)
            .single()
    }

    private fun updateOrderStatusIfNeeded(orderId: Long) {
        val totalAmount = getOrderTotalAmount(orderId)
        val paidAmount = calculatePaidAmount(orderId)
        val orderStatus = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .single()[OrdersTable.status]

        if (orderStatus == OrderStatus.COMPLETED && totalAmount > BigDecimal.ZERO && paidAmount >= totalAmount) {
            OrdersTable.update({ OrdersTable.id eq orderId }) {
                it[status] = OrderStatus.PAID
            }
        }
    }

    private fun rowToPaymentResponse(row: ResultRow): PaymentResponse {
        return PaymentResponse(
            id = row[PaymentsTable.id],
            orderId = row[PaymentsTable.orderId],
            amount = row[PaymentsTable.amount].toPlainString(),
            paymentMethod = row[PaymentsTable.paymentMethod],
            paymentStatus = row[PaymentsTable.paymentStatus],
            paidAt = row[PaymentsTable.paidAt]?.toString()
        )
    }

    private fun buildPaymentFilter(
        status: PaymentStatus?,
        method: PaymentMethod?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Op<Boolean> {
        var filter: Op<Boolean> = Op.TRUE

        if (status != null) {
            filter = filter and (PaymentsTable.paymentStatus eq status)
        }

        if (method != null) {
            filter = filter and (PaymentsTable.paymentMethod eq method)
        }

        if (from != null) {
            filter = filter and (PaymentsTable.paidAt greaterEq from)
        }

        if (to != null) {
            filter = filter and (PaymentsTable.paidAt lessEq to)
        }

        return filter
    }
}
