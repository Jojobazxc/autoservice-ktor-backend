package com.example.features.masters

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureMasterRoutes() {
    val repository = MasterRepository()

    routing {
        route("/masters") {
            get {
                call.respond(repository.getAll())
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid master id")

                val master = repository.getById(id)
                    ?: throw NotFoundException("Master not found")

                call.respond(master)
            }

            post {
                val request = call.receive<CreateMasterRequest>()
                val createdMaster = repository.create(request)
                call.respond(HttpStatusCode.Created, createdMaster)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid master id")

                val request = call.receive<UpdateMasterRequest>()
                val updated = repository.update(id, request)

                if (!updated) {
                    throw NotFoundException("Master not found")
                }

                val master = repository.getById(id)
                    ?: throw NotFoundException("Master not found")

                call.respond(master)
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid master id")

                val deleted = repository.delete(id)

                if (!deleted) {
                    throw NotFoundException("Master not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}