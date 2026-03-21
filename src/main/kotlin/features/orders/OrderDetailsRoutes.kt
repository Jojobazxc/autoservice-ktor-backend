package com.example.features.orders

import com.example.common.BadRequestException
import com.example.common.NotFoundException
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureOrderDetailsRoutes() {
    val repository = OrderDetailsRepository()

    routing {
        route("/orders") {
            get("/{id}/details") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid order id")

                val details = repository.getDetailsById(id)
                    ?: throw NotFoundException("Order not found")

                call.respond(details)
            }
        }
    }
}