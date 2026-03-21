package com.example.features.clients

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureClientRoutes() {
    val repository = ClientRepository()

    routing {
        route("/clients") {
            get {
                call.respond(repository.getAll())
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid client id")

                val client = repository.getById(id)
                    ?: throw NotFoundException("Client not found")

                call.respond(client)
            }

            post {
                val request = call.receive<CreateClientRequest>()
                val createdClient = repository.create(request)
                call.respond(HttpStatusCode.Created, createdClient)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid client id")

                val request = call.receive<UpdateClientRequest>()
                val updated = repository.update(id, request)

                if (!updated) {
                    throw NotFoundException("Client not found")
                }

                val client = repository.getById(id)
                    ?: throw NotFoundException("Client not found")

                call.respond(client)
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid client id")

                val deleted = repository.delete(id)

                if (!deleted) {
                    throw NotFoundException("Client not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}