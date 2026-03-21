package com.example.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object OrderServicesTable : Table("order_services") {
    val orderId = reference("order_id", OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val serviceId = reference("service_id", ServicesTable.id, onDelete = ReferenceOption.RESTRICT)
    val quantity = integer("quantity").default(1)
    val priceAtOrder = decimal("price_at_order", 10, 2).default(BigDecimal.ZERO)

    override val primaryKey = PrimaryKey(orderId, serviceId)

    init {
        check("order_services_quantity_check") {
            quantity greaterEq 1
        }
        check("order_services_price_at_order_check") {
            priceAtOrder greaterEq BigDecimal.ZERO
        }
    }
}