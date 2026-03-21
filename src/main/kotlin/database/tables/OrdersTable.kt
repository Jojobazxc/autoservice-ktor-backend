package com.example.database.tables

import com.example.common.enums.OrderStatus
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal

object OrdersTable : Table("orders") {
    val id = long("id").autoIncrement()
    val clientId = reference("client_id", ClientsTable.id, onDelete = ReferenceOption.RESTRICT)
    val carId = reference("car_id", CarsTable.id, onDelete = ReferenceOption.RESTRICT)
    val masterId = reference("master_id", MastersTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val description = text("description").nullable()
    val comment = text("comment").nullable()
    val status = enumerationByName("status", 20, OrderStatus::class)
    val createdAt = datetime("created_at")
    val plannedCompletionAt = datetime("planned_completion_at").nullable()
    val completedAt = datetime("completed_at").nullable()
    val totalAmount = decimal("total_amount", 10, 2).default(BigDecimal.ZERO)

    override val primaryKey = PrimaryKey(id)

    init {
        check("orders_total_amount_check") {
            totalAmount greaterEq BigDecimal.ZERO
        }
    }
}