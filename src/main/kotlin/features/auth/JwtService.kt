package com.example.features.auth

import com.auth0.jwt.JWT
import com.example.plugins.JwtConfig
import java.util.Date

class JwtService(private val config: JwtConfig) {

    fun generateToken(user: AuthUser): String {
        val now = System.currentTimeMillis()

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(user.id.toString())
            .withClaim("login", user.login)
            .withClaim("role", user.role.name)
            .withClaim("masterId", user.masterId)
            .withExpiresAt(Date(now + config.expirationMillis))
            .sign(config.algorithm)
    }
}
