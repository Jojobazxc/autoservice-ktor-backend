package com.example.features.auth

import com.example.common.ForbiddenException
import com.example.common.UnauthorizedException
import com.example.common.enums.UserRole
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route

class RoleAuthorizationConfig {
    var roles: Set<UserRole> = emptySet()
}

val RoleAuthorizationPlugin = createRouteScopedPlugin(
    name = "RoleAuthorization",
    createConfiguration = ::RoleAuthorizationConfig
) {
    val allowedRoles = pluginConfig.roles

    on(AuthenticationChecked) { call ->
        val currentRole = call.currentUserRole()
        if (currentRole !in allowedRoles) {
            throw ForbiddenException("Access denied")
        }
    }
}

fun Route.authorized(vararg roles: UserRole, build: Route.() -> Unit) {
    authenticate("auth-jwt") {
        install(RoleAuthorizationPlugin) {
            this.roles = roles.toSet()
        }
        build()
    }
}

fun ApplicationCall.currentUserRole(): UserRole {
    val role = principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("role")
        ?.asString()
        ?: throw UnauthorizedException("Unauthorized")

    return runCatching { UserRole.valueOf(role) }
        .getOrElse { throw UnauthorizedException("Unauthorized") }
}

fun ApplicationCall.currentMasterId(): Long? {
    return principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("masterId")
        ?.asLong()
}
