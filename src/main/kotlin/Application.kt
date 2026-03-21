package com.example

import com.example.database.DatabaseFactory
import com.example.database.FlywayFactory
import com.example.features.cars.configureCarRoutes
import com.example.features.clients.configureClientRoutes
import com.example.features.masters.configureMasterRoutes
import com.example.features.orders.configureOrderDetailsRoutes
import com.example.features.orders.configureOrderRoutes
import com.example.features.orders.parts.configureOrderPartRoutes
import com.example.features.orders.payments.configurePaymentRoutes
import com.example.features.orders.services.configureOrderServiceRoutes
import com.example.features.parts.configurePartRoutes
import com.example.features.services.configureServiceRoutes
import com.example.plugins.configureMonitoring
import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import com.example.plugins.configureStatusPages
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val dataSource = DatabaseFactory.init(environment.config)
    FlywayFactory.migrate(dataSource)

    configureSerialization()
    configureMonitoring()
    configureRouting()
    configureStatusPages()
    configureOrderRoutes()
    configureOrderServiceRoutes()
    configureOrderPartRoutes()
    configureOrderDetailsRoutes()
    configurePaymentRoutes()
    configureCarRoutes()
    configureClientRoutes()
    configureMasterRoutes()
    configureServiceRoutes()
    configurePartRoutes()
}
