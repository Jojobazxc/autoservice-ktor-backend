package com.example.features.orders.payments

import com.example.common.enums.PaymentMethod
import com.example.common.enums.PaymentStatus
import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val amount: String,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val paidAt: String?
)

@Serializable
data class CreatePaymentRequest(
    val amount: String,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID
)