package com.example.features.reports

import com.example.common.enums.OrderStatus
import com.example.common.enums.PaymentMethod
import com.example.common.enums.PaymentStatus
import kotlinx.serialization.Serializable

@Serializable
data class AccountingPeriodResponse(
    val from: String?,
    val to: String?
)

@Serializable
data class AccountingTotalsResponse(
    val revenue: String,
    val paid: String,
    val unpaid: String,
    val ordersCount: Int
)

@Serializable
data class AccountingPaymentStatusResponse(
    val status: PaymentStatus,
    val amount: String,
    val count: Int
)

@Serializable
data class AccountingSummaryResponse(
    val period: AccountingPeriodResponse,
    val totals: AccountingTotalsResponse,
    val byStatus: List<AccountingPaymentStatusResponse>
)

@Serializable
data class AccountingPaymentsPageResponse(
    val items: List<AccountingPaymentItemResponse>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totals: AccountingPaymentsTotalsResponse
)

@Serializable
data class AccountingPaymentsTotalsResponse(
    val amount: String,
    val count: Int,
    val paidAmount: String,
    val pendingAmount: String,
    val failedAmount: String
)

@Serializable
data class AccountingPaymentItemResponse(
    val paymentId: Long,
    val orderId: Long,
    val clientName: String,
    val car: String,
    val amount: String,
    val status: PaymentStatus,
    val method: PaymentMethod,
    val paidAt: String?
)

@Serializable
data class AccountingDebtsPageResponse(
    val items: List<AccountingDebtItemResponse>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totals: AccountingDebtsTotalsResponse
)

@Serializable
data class AccountingDebtsTotalsResponse(
    val totalAmount: String,
    val paidAmount: String,
    val debtAmount: String,
    val ordersCount: Int
)

@Serializable
data class AccountingDebtItemResponse(
    val orderId: Long,
    val clientName: String,
    val car: String,
    val totalAmount: String,
    val paidAmount: String,
    val debtAmount: String,
    val orderStatus: OrderStatus,
    val lastPaymentAt: String?
)
