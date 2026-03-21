package com.example.features.orders

import com.example.features.orders.parts.OrderPartResponse
import com.example.features.orders.payments.PaymentResponse
import com.example.features.orders.services.OrderServiceResponse
import kotlinx.serialization.Serializable

@Serializable
data class OrderDetailsResponse(
    val order: OrderResponse,
    val services: List<OrderServiceResponse>,
    val parts: List<OrderPartResponse>,
    val payments: List<PaymentResponse>
)