package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.common.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.response.respond

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val secret: String,
    val expirationMillis: Long
) {
    val algorithm: Algorithm = Algorithm.HMAC256(secret)
}

fun Application.configureAuthentication() {
    val jwtConfig = environment.config.jwtConfig()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(
                JWT
                    .require(jwtConfig.algorithm)
                    .withIssuer(jwtConfig.issuer)
                    .withAudience(jwtConfig.audience)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.subject
                val role = credential.payload.getClaim("role").asString()

                if (!userId.isNullOrBlank() && !role.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        message = "Unauthorized",
                        code = "UNAUTHORIZED"
                    )
                )
            }
        }
    }
}

fun ApplicationConfig.jwtConfig(): JwtConfig {
    return JwtConfig(
        issuer = propertyOrNull("jwt.issuer")?.getString() ?: "autoservice-backend",
        audience = propertyOrNull("jwt.audience")?.getString() ?: "autoservice-api",
        realm = propertyOrNull("jwt.realm")?.getString() ?: "autoservice",
        secret = propertyOrNull("jwt.secret")?.getString() ?: "change-me",
        expirationMillis = propertyOrNull("jwt.expirationMillis")?.getString()?.toLong() ?: 86_400_000
    )
}
