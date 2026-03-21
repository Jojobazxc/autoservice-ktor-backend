package com.example.features.orders

import com.example.common.enums.OrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val id: Long,
    val clientId: Long,
    val carId: Long,
    val masterId: Long?,
    val description: String?,
    val comment: String?,
    val status: OrderStatus,
    val createdAt: String,
    val plannedCompletionAt: String?,
    val completedAt: String?,
    val totalAmount: String
)

@Serializable
data class CreateOrderRequest(
    val clientId: Long,
    val carId: Long,
    val masterId: Long? = null,
    val description: String? = null,
    val comment: String? = null,
    val status: OrderStatus = OrderStatus.CREATED,
    val plannedCompletionAt: String? = null
)

@Serializable
data class UpdateOrderRequest(
    val clientId: Long,
    val carId: Long,
    val masterId: Long? = null,
    val description: String? = null,
    val comment: String? = null,
    val status: OrderStatus,
    val plannedCompletionAt: String? = null,
    val completedAt: String? = null
)