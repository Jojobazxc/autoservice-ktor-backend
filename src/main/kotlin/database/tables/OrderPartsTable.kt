package com.example.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object OrderPartsTable : Table("order_parts") {
    val orderId = reference("order_id", OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val partId = reference("part_id", PartsTable.id, onDelete = ReferenceOption.RESTRICT)
    val quantity = integer("quantity").default(1)
    val priceAtOrder = decimal("price_at_order", 10, 2).default(BigDecimal.ZERO)

    override val primaryKey = PrimaryKey(orderId, partId)

    init {
        check("order_parts_quantity_check") {
            quantity greaterEq 1
        }
        check("order_parts_price_at_order_check") {
            priceAtOrder greaterEq BigDecimal.ZERO
        }
    }
}