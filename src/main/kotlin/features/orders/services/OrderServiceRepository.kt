package com.example.features.orders.services

import com.example.database.tables.OrderPartsTable
import com.example.database.tables.OrderServicesTable
import com.example.database.tables.OrdersTable
import com.example.database.tables.ServicesTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class OrderServiceRepository {

    fun getAllByOrderId(orderId: Long): List<OrderServiceResponse> = transaction {
        OrderServicesTable
            .selectAll()
            .where { OrderServicesTable.orderId eq orderId }
            .map(::rowToOrderServiceResponse)
    }

    fun orderExists(orderId: Long): Boolean = transaction {
        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .any()
    }

    fun serviceExists(serviceId: Long): Boolean = transaction {
        ServicesTable
            .selectAll()
            .where { ServicesTable.id eq serviceId }
            .any()
    }

    fun itemExists(orderId: Long, serviceId: Long): Boolean = transaction {
        OrderServicesTable
            .selectAll()
            .where { (OrderServicesTable.orderId eq orderId) and (OrderServicesTable.serviceId eq serviceId) }
            .any()
    }

    fun getServiceBasePrice(serviceId: Long): BigDecimal = transaction {
        ServicesTable
            .selectAll()
            .where { ServicesTable.id eq serviceId }
            .single()[ServicesTable.basePrice]
    }

    fun add(orderId: Long, request: AddOrderServiceRequest): OrderServiceResponse = transaction {
        val actualPrice = request.priceAtOrder?.toBigDecimal() ?: getServiceBasePrice(request.serviceId)

        OrderServicesTable.insert {
            it[OrderServicesTable.orderId] = orderId
            it[serviceId] = request.serviceId
            it[quantity] = request.quantity
            it[priceAtOrder] = actualPrice
        }

        recalculateOrderTotal(orderId)

        OrderServicesTable
            .selectAll()
            .where {
                (OrderServicesTable.orderId eq orderId) and
                        (OrderServicesTable.serviceId eq request.serviceId)
            }
            .map(::rowToOrderServiceResponse)
            .single()
    }

    fun update(orderId: Long, serviceId: Long, request: UpdateOrderServiceRequest): Boolean = transaction {
        val updatedRows = OrderServicesTable.update({
            (OrderServicesTable.orderId eq orderId) and
                    (OrderServicesTable.serviceId eq serviceId)
        }) {
            it[quantity] = request.quantity
            it[priceAtOrder] = request.priceAtOrder.toBigDecimal()
        }

        if (updatedRows > 0) {
            recalculateOrderTotal(orderId)
        }

        updatedRows > 0
    }

    fun delete(orderId: Long, serviceId: Long): Boolean = transaction {
        val deletedRows = OrderServicesTable.deleteWhere {
            (OrderServicesTable.orderId eq orderId) and
                    (OrderServicesTable.serviceId eq serviceId)
        }

        if (deletedRows > 0) {
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

    private fun rowToOrderServiceResponse(row: ResultRow): OrderServiceResponse {
        return OrderServiceResponse(
            orderId = row[OrderServicesTable.orderId],
            serviceId = row[OrderServicesTable.serviceId],
            quantity = row[OrderServicesTable.quantity],
            priceAtOrder = row[OrderServicesTable.priceAtOrder].toPlainString()
        )
    }
}