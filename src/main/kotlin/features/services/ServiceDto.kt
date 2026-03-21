package com.example.features.services

import kotlinx.serialization.Serializable

@Serializable
data class ServiceResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val basePrice: String,
    val normHours: String?
)

@Serializable
data class CreateServiceRequest(
    val name: String,
    val description: String? = null,
    val basePrice: String,
    val normHours: String? = null
)

@Serializable
data class UpdateServiceRequest(
    val name: String,
    val description: String? = null,
    val basePrice: String,
    val normHours: String? = null
)