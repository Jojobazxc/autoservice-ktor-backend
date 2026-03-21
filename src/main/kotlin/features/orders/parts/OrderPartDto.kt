package com.example.features.orders.parts

import kotlinx.serialization.Serializable

@Serializable
data class OrderPartResponse(
    val orderId: Long,
    val partId: Long,
    val quantity: Int,
    val priceAtOrder: String
)

@Serializable
data class AddOrderPartRequest(
    val partId: Long,
    val quantity: Int = 1,
    val priceAtOrder: String? = null
)

@Serializable
data class UpdateOrderPartRequest(
    val quantity: Int,
    val priceAtOrder: String
)