package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.or
import java.math.BigDecimal

object ServicesTable : Table("services") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val basePrice = decimal("base_price", 10, 2).default(BigDecimal.ZERO)
    val normHours = decimal("norm_hours", 5, 2).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        check("services_base_price_check") {
            basePrice greaterEq BigDecimal.ZERO
        }
        check("services_norm_hours_check") {
            normHours.isNull() or (normHours greaterEq BigDecimal.ZERO)
        }
    }
}