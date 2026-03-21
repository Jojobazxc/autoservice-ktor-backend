package com.example.features.orders.parts

import com.example.common.BadRequestException
import com.example.database.tables.OrderPartsTable
import com.example.database.tables.OrderServicesTable
import com.example.database.tables.OrdersTable
import com.example.database.tables.PartsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class OrderPartRepository {

    fun getAllByOrderId(orderId: Long): List<OrderPartResponse> = transaction {
        OrderPartsTable
            .selectAll()
            .where { OrderPartsTable.orderId eq orderId }
            .map(::rowToOrderPartResponse)
    }

    fun orderExists(orderId: Long): Boolean = transaction {
        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .any()
    }

    fun partExists(partId: Long): Boolean = transaction {
        PartsTable
            .selectAll()
            .where { PartsTable.id eq partId }
            .any()
    }

    fun itemExists(orderId: Long, partId: Long): Boolean = transaction {
        OrderPartsTable
            .selectAll()
            .where { (OrderPartsTable.orderId eq orderId) and (OrderPartsTable.partId eq partId) }
            .any()
    }

    fun getPartPrice(partId: Long): BigDecimal = transaction {
        PartsTable
            .selectAll()
            .where { PartsTable.id eq partId }
            .single()[PartsTable.price]
    }

    fun getPartStock(partId: Long): Int = transaction {
        PartsTable
            .selectAll()
            .where { PartsTable.id eq partId }
            .single()[PartsTable.stockQuantity]
    }

    fun getCurrentItemQuantity(orderId: Long, partId: Long): Int = transaction {
        OrderPartsTable
            .selectAll()
            .where { (OrderPartsTable.orderId eq orderId) and (OrderPartsTable.partId eq partId) }
            .single()[OrderPartsTable.quantity]
    }

    fun add(orderId: Long, request: AddOrderPartRequest): OrderPartResponse = transaction {
        val actualPrice = request.priceAtOrder?.toBigDecimal() ?: getPartPrice(request.partId)
        val currentStock = getPartStock(request.partId)

        if (currentStock < request.quantity) {
            throw BadRequestException("Not enough stock for the requested part")
        }

        OrderPartsTable.insert {
            it[OrderPartsTable.orderId] = orderId
            it[partId] = request.partId
            it[quantity] = request.quantity
            it[priceAtOrder] = actualPrice
        }

        PartsTable.update({ PartsTable.id eq request.partId }) {
            it[stockQuantity] = currentStock - request.quantity
        }

        recalculateOrderTotal(orderId)

        OrderPartsTable
            .selectAll()
            .where {
                (OrderPartsTable.orderId eq orderId) and
                        (OrderPartsTable.partId eq request.partId)
            }
            .map(::rowToOrderPartResponse)
            .single()
    }

    fun update(orderId: Long, partId: Long, request: UpdateOrderPartRequest): Boolean = transaction {
        val currentQuantity = getCurrentItemQuantity(orderId, partId)
        val currentStock = getPartStock(partId)
        val diff = request.quantity - currentQuantity

        if (diff > 0 && currentStock < diff) {
            throw BadRequestException("Not enough stock for the requested part quantity")
        }

        val updatedRows = OrderPartsTable.update({
            (OrderPartsTable.orderId eq orderId) and
                    (OrderPartsTable.partId eq partId)
        }) {
            it[quantity] = request.quantity
            it[priceAtOrder] = request.priceAtOrder.toBigDecimal()
        }

        if (updatedRows > 0) {
            PartsTable.update({ PartsTable.id eq partId }) {
                it[stockQuantity] = currentStock - diff
            }
            recalculateOrderTotal(orderId)
        }

        updatedRows > 0
    }

    fun delete(orderId: Long, partId: Long): Boolean = transaction {
        val currentQuantity = getCurrentItemQuantity(orderId, partId)
        val currentStock = getPartStock(partId)

        val deletedRows = OrderPartsTable.deleteWhere {
            (OrderPartsTable.orderId eq orderId) and
                    (OrderPartsTable.partId eq partId)
        }

        if (deletedRows > 0) {
            PartsTable.update({ PartsTable.id eq partId }) {
                it[stockQuantity] = currentStock + currentQuantity
            }
            recalculateOrderTotal(orderId)
        }

        deletedRows > 0
    }

    private fun recalculateOrderTotal(orderId: Long) {
        val serviceSum = OrderServicesTable
            .selectAll()
            .where { OrderServicesTable.orderId eq orderId }
            .sumOf {
                it[OrderServicesTable.priceAtOrder].multiply(BigDecimal(it[OrderServicesTable.quantity]))
            }

        val partsSum = OrderPartsTable
            .selectAll()
            .where { OrderPartsTable.orderId eq orderId }
            .sumOf {
                it[OrderPartsTable.priceAtOrder].multiply(BigDecimal(it[OrderPartsTable.quantity]))
            }

        OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[totalAmount] = serviceSum + partsSum
        }
    }

    private fun rowToOrderPartResponse(row: ResultRow): OrderPartResponse {
        return OrderPartResponse(
            orderId = row[OrderPartsTable.orderId],
            partId = row[OrderPartsTable.partId],
            quantity = row[OrderPartsTable.quantity],
            priceAtOrder = row[OrderPartsTable.priceAtOrder].toPlainString()
        )
    }
}