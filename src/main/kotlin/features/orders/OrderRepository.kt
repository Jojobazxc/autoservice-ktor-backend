package com.example.features.orders

import com.example.common.enums.OrderStatus
import com.example.database.tables.CarsTable
import com.example.database.tables.ClientsTable
import com.example.database.tables.MastersTable
import com.example.database.tables.OrdersTable
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
            it[status] = request.status
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
    }

    fun update(id: Long, request: UpdateOrderRequest): Boolean = transaction {
        val updatedRows = OrdersTable.update({ OrdersTable.id eq id }) {
            it[clientId] = request.clientId
            it[carId] = request.carId
            it[masterId] = request.masterId
            it[description] = request.description
            it[comment] = request.comment
            it[status] = request.status
            it[plannedCompletionAt] = request.plannedCompletionAt?.let(LocalDateTime::parse)
            it[completedAt] = request.completedAt?.let(LocalDateTime::parse)
        }
        updatedRows > 0
    }

    fun delete(id: Long): Boolean = transaction {
        val deletedRows = OrdersTable.deleteWhere { OrdersTable.id eq id }
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