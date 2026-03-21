package com.example.features.orders.payments

import com.example.common.enums.OrderStatus
import com.example.common.enums.PaymentStatus
import com.example.database.tables.OrdersTable
import com.example.database.tables.PaymentsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentRepository {

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
        PaymentsTable
            .selectAll()
            .where {
                (PaymentsTable.orderId eq orderId) and
                        (PaymentsTable.paymentStatus eq PaymentStatus.PAID)
            }
            .sumOf { it[PaymentsTable.amount] }
    }

    fun create(orderId: Long, request: CreatePaymentRequest): PaymentResponse = transaction {
        val now = if (request.paymentStatus == PaymentStatus.PAID) LocalDateTime.now() else null

        val createdId = PaymentsTable.insert {
            it[PaymentsTable.orderId] = orderId
            it[amount] = request.amount.toBigDecimal()
            it[paymentMethod] = request.paymentMethod
            it[paymentStatus] = request.paymentStatus
            it[paidAt] = now
        }[PaymentsTable.id]

        updateOrderStatusIfNeeded(orderId)

        PaymentsTable
            .selectAll()
            .where { PaymentsTable.id eq createdId }
            .map(::rowToPaymentResponse)
            .single()
    }

    private fun updateOrderStatusIfNeeded(orderId: Long) {
        val totalAmount = getOrderTotalAmount(orderId)
        val paidAmount = getPaidAmount(orderId)

        if (totalAmount > BigDecimal.ZERO && paidAmount >= totalAmount) {
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
}