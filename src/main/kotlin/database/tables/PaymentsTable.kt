package com.example.database.tables

import com.example.common.enums.PaymentMethod
import com.example.common.enums.PaymentStatus
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal

object PaymentsTable : Table("payments") {
    val id = long("id").autoIncrement()
    val orderId = reference("order_id", OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val amount = decimal("amount", 10, 2).default(BigDecimal.ZERO)
    val paymentMethod = enumerationByName("payment_method", 20, PaymentMethod::class)
    val paymentStatus = enumerationByName("payment_status", 20, PaymentStatus::class)
    val paidAt = datetime("paid_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        check("payments_amount_check") {
            amount greaterEq BigDecimal.ZERO
        }
    }
}