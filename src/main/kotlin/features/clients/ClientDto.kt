package com.example.features.clients

import com.example.common.enums.ClientStatus
import kotlinx.serialization.Serializable

@Serializable
data class ClientResponse(
    val id: Long,
    val fullName: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val registrationDate: String,
    val status: ClientStatus
)

@Serializable
data class CreateClientRequest(
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val status: ClientStatus = ClientStatus.REGULAR
)

@Serializable
data class UpdateClientRequest(
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val status: ClientStatus
)