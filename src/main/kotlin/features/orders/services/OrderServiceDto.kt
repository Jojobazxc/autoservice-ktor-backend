package com.example.features.orders.services

import kotlinx.serialization.Serializable

@Serializable
data class OrderServiceResponse(
    val orderId: Long,
    val serviceId: Long,
    val quantity: Int,
    val priceAtOrder: String
)

@Serializable
data class AddOrderServiceRequest(
    val serviceId: Long,
    val quantity: Int = 1,
    val priceAtOrder: String? = null
)

@Serializable
data class UpdateOrderServiceRequest(
    val quantity: Int,
    val priceAtOrder: String
)