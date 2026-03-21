package com.example.features.cars

import kotlinx.serialization.Serializable

@Serializable
data class CarResponse(
    val id: Long,
    val clientId: Long,
    val brand: String,
    val model: String,
    val year: Int?,
    val plateNumber: String,
    val vin: String?,
    val mileage: Int?
)

@Serializable
data class CreateCarRequest(
    val clientId: Long,
    val brand: String,
    val model: String,
    val year: Int? = null,
    val plateNumber: String,
    val vin: String? = null,
    val mileage: Int? = null
)

@Serializable
data class UpdateCarRequest(
    val clientId: Long,
    val brand: String,
    val model: String,
    val year: Int? = null,
    val plateNumber: String,
    val vin: String? = null,
    val mileage: Int? = null
)