package com.example.features.orders

import com.example.database.tables.OrderPartsTable
import com.example.database.tables.OrderServicesTable
import com.example.database.tables.OrdersTable
import com.example.database.tables.PaymentsTable
import com.example.features.orders.parts.OrderPartResponse
import com.example.features.orders.payments.PaymentResponse
import com.example.features.orders.services.OrderServiceResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class OrderDetailsRepository {

    fun getDetailsById(orderId: Long): OrderDetailsResponse? = transaction {
        val order = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .map(::rowToOrderResponse)
            .singleOrNull()
            ?: return@transaction null

        val services = OrderServicesTable
            .selectAll()
            .where { OrderServicesTable.orderId eq orderId }
            .map(::rowToOrderServiceResponse)

        val parts = OrderPartsTable
            .selectAll()
            .where { OrderPartsTable.orderId eq orderId }
            .map(::rowToOrderPartResponse)

        val payments = PaymentsTable
            .selectAll()
            .where { PaymentsTable.orderId eq orderId }
            .map(::rowToPaymentResponse)

        OrderDetailsResponse(
            order = order,
            services = services,
            parts = parts,
            payments = payments
        )
    }

    private fun rowToOrderResponse(row: ResultRow): OrderResponse {
        return OrderResponse(
            id = row[OrdersTable.id],
            clientId = row[OrdersTable.clientId],
            carId = row[OrdersTable.carId],
            masterId = row[OrdersTable.masterId],
            description = row[OrdersTable.description],
            comment = row[OrdersTable.comment],
            status = row[OrdersTable.status],
            createdAt = row[OrdersTable.createdAt].toString(),
            plannedCompletionAt = row[OrdersTable.plannedCompletionAt]?.toString(),
            completedAt = row[OrdersTable.completedAt]?.toString(),
            totalAmount = row[OrdersTable.totalAmount].toPlainString()
        )
    }

    private fun rowToOrderServiceResponse(row: ResultRow): OrderServiceResponse {
        return OrderServiceResponse(
            orderId = row[OrderServicesTable.orderId],
            serviceId = row[OrderServicesTable.serviceId],
            quantity = row[OrderServicesTable.quantity],
            priceAtOrder = row[OrderServicesTable.priceAtOrder].toPlainString()
        )
    }

    private fun rowToOrderPartResponse(row: ResultRow): OrderPartResponse {
        return OrderPartResponse(
            orderId = row[OrderPartsTable.orderId],
            partId = row[OrderPartsTable.partId],
            quantity = row[OrderPartsTable.quantity],
            priceAtOrder = row[OrderPartsTable.priceAtOrder].toPlainString()
        )
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