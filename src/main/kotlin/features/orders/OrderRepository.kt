package com.example.features.orders

import com.example.common.ConflictException
import com.example.common.ForbiddenException
import com.example.common.enums.EmploymentStatus
import com.example.common.enums.OrderStatus
import com.example.database.tables.CarsTable
import com.example.database.tables.ClientsTable
import com.example.database.tables.MastersTable
import com.example.database.tables.OrdersTable
import com.example.database.tables.PaymentsTable
import com.example.common.enums.PaymentStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

class OrderRepository {

    fun getAll(clientId: Long?, status: OrderStatus?): List<OrderResponse> = transaction {
        val query = OrdersTable.selectAll()

        when {
            clientId != null && status != null -> {
                query.where {
                    (OrdersTable.clientId eq clientId) and (OrdersTable.status eq status)
                }
            }

            clientId != null -> {
                query.where { OrdersTable.clientId eq clientId }
            }

            status != null -> {
                query.where { OrdersTable.status eq status }
            }
        }

        query.map(::rowToOrderResponse)
    }

    fun getById(id: Long): OrderResponse? = transaction {
        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id }
            .map(::rowToOrderResponse)
            .singleOrNull()
    }

    fun create(request: CreateOrderRequest): OrderResponse = transaction {
        val createdId = OrdersTable.insert {
            it[clientId] = request.clientId
            it[carId] = request.carId
            it[masterId] = request.masterId
            it[description] = request.description
            it[comment] = request.comment
            it[status] = if (request.masterId != null) OrderStatus.IN_PROGRESS else OrderStatus.CREATED
            it[createdAt] = LocalDateTime.now()
            it[plannedCompletionAt] = request.plannedCompletionAt?.let(LocalDateTime::parse)
            it[completedAt] = null
            it[totalAmount] = BigDecimal.ZERO
        }[OrdersTable.id]

        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq createdId }
            .map(::rowToOrderResponse)
            .single()
            .also { syncMasterStatus(it.masterId) }
    }

    fun update(id: Long, request: UpdateOrderRequest): Boolean = transaction {
        val currentOrder = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id }
            .singleOrNull()
            ?: return@transaction false

        val currentStatus = currentOrder[OrdersTable.status]
        val previousMasterId = currentOrder[OrdersTable.masterId]
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.PAID || currentStatus == OrderStatus.CANCELED) {
            throw ConflictException("Completed, paid or canceled order cannot be updated")
        }

        val nextStatus = if (currentStatus == OrderStatus.CREATED && request.masterId != null) {
            OrderStatus.IN_PROGRESS
        } else {
            currentStatus
        }

        val updatedRows = OrdersTable.update({ OrdersTable.id eq id }) {
            it[clientId] = request.clientId
            it[carId] = request.carId
            it[masterId] = request.masterId
            it[description] = request.description
            it[comment] = request.comment
            it[status] = nextStatus
            it[plannedCompletionAt] = request.plannedCompletionAt?.let(LocalDateTime::parse)
        }
        if (updatedRows > 0) {
            syncMasterStatus(previousMasterId)
            syncMasterStatus(request.masterId)
        }
        updatedRows > 0
    }

    fun complete(id: Long, assignedMasterId: Long?): OrderResponse? = transaction {
        val order = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id }
            .singleOrNull()
            ?: return@transaction null

        if (assignedMasterId != null && order[OrdersTable.masterId] != assignedMasterId) {
            throw ForbiddenException("Only assigned master can complete this order")
        }

        if (order[OrdersTable.masterId] == null) {
            throw ConflictException("Order cannot be completed without assigned master")
        }

        if (order[OrdersTable.status] != OrderStatus.IN_PROGRESS) {
            throw ConflictException("Only in-progress order can be completed")
        }

        val nextStatus = if (isOrderFullyPaid(id, order[OrdersTable.totalAmount])) {
            OrderStatus.PAID
        } else {
            OrderStatus.COMPLETED
        }

        OrdersTable.update({ OrdersTable.id eq id }) {
            it[status] = nextStatus
            it[completedAt] = LocalDateTime.now()
        }
        syncMasterStatus(order[OrdersTable.masterId])

        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id }
            .map(::rowToOrderResponse)
            .single()
    }

    fun cancel(id: Long): OrderResponse? = transaction {
        val order = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id }
            .singleOrNull()
            ?: return@transaction null

        if (order[OrdersTable.status] == OrderStatus.PAID) {
            throw ConflictException("Paid order cannot be canceled")
        }

        if (order[OrdersTable.status] == OrderStatus.CANCELED) {
            throw ConflictException("Order is already canceled")
        }

        OrdersTable.update({ OrdersTable.id eq id }) {
            it[status] = OrderStatus.CANCELED
        }
        syncMasterStatus(order[OrdersTable.masterId])

        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id }
            .map(::rowToOrderResponse)
            .single()
    }

    fun delete(id: Long): Boolean = transaction {
        val masterId = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id }
            .singleOrNull()
            ?.get(OrdersTable.masterId)
        val deletedRows = OrdersTable.deleteWhere { OrdersTable.id eq id }
        if (deletedRows > 0) {
            syncMasterStatus(masterId)
        }
        deletedRows > 0
    }

    fun clientExists(clientId: Long): Boolean = transaction {
        ClientsTable
            .selectAll()
            .where { ClientsTable.id eq clientId }
            .any()
    }

    fun carExists(carId: Long): Boolean = transaction {
        CarsTable
            .selectAll()
            .where { CarsTable.id eq carId }
            .any()
    }

    fun masterExists(masterId: Long): Boolean = transaction {
        MastersTable
            .selectAll()
            .where { MastersTable.id eq masterId }
            .any()
    }

    fun carBelongsToClient(carId: Long, clientId: Long): Boolean = transaction {
        CarsTable
            .selectAll()
            .where { (CarsTable.id eq carId) and (CarsTable.clientId eq clientId) }
            .any()
    }

    private fun isOrderFullyPaid(orderId: Long, totalAmount: BigDecimal): Boolean {
        if (totalAmount <= BigDecimal.ZERO) {
            return false
        }

        val paidAmount = PaymentsTable
            .selectAll()
            .where {
                (PaymentsTable.orderId eq orderId) and
                        (PaymentsTable.paymentStatus eq PaymentStatus.PAID)
            }
            .sumOf { it[PaymentsTable.amount] }

        return paidAmount >= totalAmount
    }

    private fun syncMasterStatus(masterId: Long?) {
        if (masterId == null) {
            return
        }

        val hasActiveOrder = OrdersTable
            .selectAll()
            .where {
                (OrdersTable.masterId eq masterId) and
                        ((OrdersTable.status eq OrderStatus.CREATED) or
                                (OrdersTable.status eq OrderStatus.IN_PROGRESS))
            }
            .any()

        MastersTable.update({ MastersTable.id eq masterId }) {
            it[employmentStatus] = if (hasActiveOrder) EmploymentStatus.BUSY else EmploymentStatus.FREE
        }
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
}
