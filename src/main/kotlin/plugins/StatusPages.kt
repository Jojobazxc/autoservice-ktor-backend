package com.example.plugins

import com.example.common.BadRequestException
import com.example.common.ConflictException
import com.example.common.ErrorResponse
import com.example.common.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.postgresql.util.PSQLException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    message = cause.message ?: "Bad request",
                    code = "BAD_REQUEST"
                )
            )
        }

        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    message = cause.message ?: "Resource not found",
                    code = "NOT_FOUND"
                )
            )
        }

        exception<ConflictException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    message = cause.message ?: "Conflict",
                    code = "CONFLICT"
                )
            )
        }

        exception<ExposedSQLException> { call, cause ->
            val pgCause = cause.cause as? PSQLException
            if (pgCause?.sqlState == "23505") {
                call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse(
                        message = mapUniqueViolationMessage(pgCause.message ?: cause.message.orEmpty()),
                        code = "UNIQUE_CONSTRAINT_VIOLATION"
                    )
                )
            } else {
                cause.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        message = "Internal server error",
                        code = "INTERNAL_SERVER_ERROR"
                    )
                )
            }
        }

        exception<PSQLException> { call, cause ->
            if (cause.sqlState == "23505") {
                call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse(
                        message = mapUniqueViolationMessage(cause.message.orEmpty()),
                        code = "UNIQUE_CONSTRAINT_VIOLATION"
                    )
                )
            } else {
                cause.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        message = "Internal server error",
                        code = "INTERNAL_SERVER_ERROR"
                    )
                )
            }
        }

        exception<Throwable> { call, cause ->
            cause.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    message = "Internal server error",
                    code = "INTERNAL_SERVER_ERROR"
                )
            )
        }
    }
}

private fun mapUniqueViolationMessage(rawMessage: String): String {
    val lower = rawMessage.lowercase()

    return when {
        "clients_phone_key" in lower || " phone " in lower ->
            "Клиент с таким номером телефона уже существует"

        "cars_plate_number_key" in lower || "plate_number" in lower ->
            "Машина с таким номерным знаком уже существует"

        "cars_vin_key" in lower || " vin " in lower ->
            "Машина с таким VIN уже существует"

        "services_name_key" in lower || "services" in lower && "name" in lower ->
            "Услуга с таким названием уже существует"

        "parts_article_key" in lower || "article" in lower ->
            "Запчасть с таким артикулом существует"

        else ->
            "Ресурс с уникальными полями уже существует"
    }
}