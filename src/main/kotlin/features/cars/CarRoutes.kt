package com.example.features.cars

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCarRoutes() {
    val repository = CarRepository()

    routing {
        route("/cars") {
            get {
                call.respond(repository.getAll())
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid car id")

                val car = repository.getById(id)
                    ?: throw NotFoundException("Car not found")

                call.respond(car)
            }

            post {
                val request = call.receive<CreateCarRequest>()

                if (!repository.clientExists(request.clientId)) {
                    throw NotFoundException("Client not found")
                }

                val createdCar = repository.create(request)
                call.respond(HttpStatusCode.Created, createdCar)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid car id")

                val request = call.receive<UpdateCarRequest>()

                if (!repository.clientExists(request.clientId)) {
                    throw NotFoundException("Client not found")
                }

                val updated = repository.update(id, request)
                if (!updated) {
                    throw NotFoundException("Car not found")
                }

                val car = repository.getById(id)
                    ?: throw NotFoundException("Car not found")

                call.respond(car)
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid car id")

                val deleted = repository.delete(id)
                if (!deleted) {
                    throw NotFoundException("Car not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }

        route("/clients/{clientId}/cars") {
            get {
                val clientId = call.parameters["clientId"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid client id")

                if (!repository.clientExists(clientId)) {
                    throw NotFoundException("Client not found")
                }

                call.respond(repository.getByClientId(clientId))
            }
        }
    }
}