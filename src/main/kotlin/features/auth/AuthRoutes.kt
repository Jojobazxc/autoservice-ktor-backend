package com.example.features.auth

import com.example.common.UnauthorizedException
import com.example.plugins.jwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureAuthRoutes() {
    val repository = AuthRepository()
    val jwtConfig = environment.config.jwtConfig()
    val jwtService = JwtService(jwtConfig)

    routing {
        post("/auth/login") {
            val request = call.receive<LoginRequest>()
            val user = repository.findActiveByLogin(request.login)

            if (user == null || !PasswordHasher.verify(request.password, user.passwordHash)) {
                throw UnauthorizedException("Invalid login or password")
            }

            call.respond(
                HttpStatusCode.OK,
                TokenResponse(
                    token = jwtService.generateToken(user),
                    expiresInMillis = jwtConfig.expirationMillis,
                    role = user.role
                )
            )
        }
    }
}
