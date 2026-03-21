package com.example.features.parts

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.math.BigDecimal

fun Application.configurePartRoutes() {
    val repository = PartRepository()

    routing {
        route("/parts") {
            get {
                call.respond(repository.getAll())
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid part id")

                val part = repository.getById(id)
                    ?: throw NotFoundException("Part not found")

                call.respond(part)
            }

            post {
                val request = call.receive<CreatePartRequest>()
                validatePartRequest(
                    name = request.name,
                    article = request.article,
                    price = request.price,
                    unit = request.unit,
                    stockQuantity = request.stockQuantity
                )

                val createdPart = repository.create(request)
                call.respond(HttpStatusCode.Created, createdPart)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid part id")

                val request = call.receive<UpdatePartRequest>()
                validatePartRequest(
                    name = request.name,
                    article = request.article,
                    price = request.price,
                    unit = request.unit,
                    stockQuantity = request.stockQuantity
                )

                val updated = repository.update(id, request)
                if (!updated) {
                    throw NotFoundException("Part not found")
                }

                val part = repository.getById(id)
                    ?: throw NotFoundException("Part not found")

                call.respond(part)
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid part id")

                val deleted = repository.delete(id)
                if (!deleted) {
                    throw NotFoundException("Part not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun validatePartRequest(
    name: String,
    article: String,
    price: String,
    unit: String,
    stockQuantity: Int
) {
    if (name.isBlank()) {
        throw BadRequestException("Part name must not be blank")
    }

    if (article.isBlank()) {
        throw BadRequestException("Article must not be blank")
    }

    if (unit.isBlank()) {
        throw BadRequestException("Unit must not be blank")
    }

    val parsedPrice = try {
        price.toBigDecimal()
    } catch (_: Exception) {
        throw BadRequestException("price must be a valid decimal number")
    }

    if (parsedPrice < BigDecimal.ZERO) {
        throw BadRequestException("price must be greater than or equal to 0")
    }

    if (stockQuantity < 0) {
        throw BadRequestException("stockQuantity must be greater than or equal to 0")
    }
}