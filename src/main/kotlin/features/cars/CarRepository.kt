package com.example.features.cars

import com.example.database.tables.CarsTable
import com.example.database.tables.ClientsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CarRepository {

    fun getAll(): List<CarResponse> = transaction {
        CarsTable
            .selectAll()
            .map(::rowToCarResponse)
    }

    fun getById(id: Long): CarResponse? = transaction {
        CarsTable
            .selectAll()
            .where { CarsTable.id eq id }
            .map(::rowToCarResponse)
            .singleOrNull()
    }

    fun getByClientId(clientId: Long): List<CarResponse> = transaction {
        CarsTable
            .selectAll()
            .where { CarsTable.clientId eq clientId }
            .map(::rowToCarResponse)
    }

    fun clientExists(clientId: Long): Boolean = transaction {
        ClientsTable
            .selectAll()
            .where { ClientsTable.id eq clientId }
            .any()
    }

    fun create(request: CreateCarRequest): CarResponse = transaction {
        val createdId = CarsTable.insert {
            it[clientId] = request.clientId
            it[brand] = request.brand
            it[model] = request.model
            it[year] = request.year
            it[plateNumber] = request.plateNumber
            it[vin] = request.vin
            it[mileage] = request.mileage
        }[CarsTable.id]

        CarsTable
            .selectAll()
            .where { CarsTable.id eq createdId }
            .map(::rowToCarResponse)
            .single()
    }

    fun update(id: Long, request: UpdateCarRequest): Boolean = transaction {
        val updatedRows = CarsTable.update({ CarsTable.id eq id }) {
            it[clientId] = request.clientId
            it[brand] = request.brand
            it[model] = request.model
            it[year] = request.year
            it[plateNumber] = request.plateNumber
            it[vin] = request.vin
            it[mileage] = request.mileage
        }
        updatedRows > 0
    }

    fun delete(id: Long): Boolean = transaction {
        val deletedRows = CarsTable.deleteWhere { CarsTable.id eq id }
        deletedRows > 0
    }

    private fun rowToCarResponse(row: ResultRow): CarResponse {
        return CarResponse(
            id = row[CarsTable.id],
            clientId = row[CarsTable.clientId],
            brand = row[CarsTable.brand],
            model = row[CarsTable.model],
            year = row[CarsTable.year],
            plateNumber = row[CarsTable.plateNumber],
            vin = row[CarsTable.vin],
            mileage = row[CarsTable.mileage]
        )
    }
}