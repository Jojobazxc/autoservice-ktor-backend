package com.example.features.services

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal

fun Application.configureServiceRoutes() {
    val repository = ServiceRepository()

    routing {
        route("/services") {
            get {
                call.respond(repository.getAll())
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid service id")

                val service = repository.getById(id)
                    ?: throw NotFoundException("Service not found")

                call.respond(service)
            }

            post {
                val request = call.receive<CreateServiceRequest>()
                validateServiceRequest(request.name, request.basePrice, request.normHours)

                val createdService = repository.create(request)
                call.respond(HttpStatusCode.Created, createdService)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid service id")

                val request = call.receive<UpdateServiceRequest>()
                validateServiceRequest(request.name, request.basePrice, request.normHours)

                val updated = repository.update(id, request)
                if (!updated) {
                    throw NotFoundException("Service not found")
                }

                val service = repository.getById(id)
                    ?: throw NotFoundException("Service not found")

                call.respond(service)
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid service id")

                val deleted = repository.delete(id)
                if (!deleted) {
                    throw NotFoundException("Service not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun validateServiceRequest(
    name: String,
    basePrice: String,
    normHours: String?
) {
    if (name.isBlank()) {
        throw BadRequestException("Service name must not be blank")
    }

    val parsedBasePrice = try {
        basePrice.toBigDecimal()
    } catch (_: Exception) {
        throw BadRequestException("basePrice must be a valid decimal number")
    }

    if (parsedBasePrice < BigDecimal.ZERO) {
        throw BadRequestException("basePrice must be greater than or equal to 0")
    }

    val parsedNormHours = normHours?.let {
        try {
            it.toBigDecimal()
        } catch (_: Exception) {
            throw BadRequestException("normHours must be a valid decimal number")
        }
    }

    if (parsedNormHours != null && parsedNormHours < BigDecimal.ZERO) {
        throw BadRequestException("normHours must be greater than or equal to 0")
    }
}