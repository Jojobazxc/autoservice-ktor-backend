package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object PartsTable : Table("parts") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val article = varchar("article", 100).uniqueIndex()
    val price = decimal("price", 10, 2).default(BigDecimal.ZERO)
    val unit = varchar("unit", 50).default("pcs")
    val stockQuantity = integer("stock_quantity").default(0)

    override val primaryKey = PrimaryKey(id)

    init {
        check("parts_price_check") {
            price greaterEq BigDecimal.ZERO
        }
        check("parts_stock_quantity_check") {
            stockQuantity greaterEq 0
        }
    }
}