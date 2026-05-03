package com.example.features.auth

import com.example.common.enums.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class TokenResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresInMillis: Long,
    val role: UserRole
)

data class AuthUser(
    val id: Long,
    val login: String,
    val passwordHash: String,
    val role: UserRole,
    val masterId: Long?
)
