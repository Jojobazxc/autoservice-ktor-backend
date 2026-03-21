package com.example.features.services

import com.example.database.tables.ServicesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ServiceRepository {

    fun getAll(): List<ServiceResponse> = transaction {
        ServicesTable
            .selectAll()
            .map(::rowToServiceResponse)
    }

    fun getById(id: Long): ServiceResponse? = transaction {
        ServicesTable
            .selectAll()
            .where { ServicesTable.id eq id }
            .map(::rowToServiceResponse)
            .singleOrNull()
    }

    fun create(request: CreateServiceRequest): ServiceResponse = transaction {
        val createdId = ServicesTable.insert {
            it[name] = request.name
            it[description] = request.description
            it[basePrice] = request.basePrice.toBigDecimal()
            it[normHours] = request.normHours?.toBigDecimal()
        }[ServicesTable.id]

        ServicesTable
            .selectAll()
            .where { ServicesTable.id eq createdId }
            .map(::rowToServiceResponse)
            .single()
    }

    fun update(id: Long, request: UpdateServiceRequest): Boolean = transaction {
        val updatedRows = ServicesTable.update({ ServicesTable.id eq id }) {
            it[name] = request.name
            it[description] = request.description
            it[basePrice] = request.basePrice.toBigDecimal()
            it[normHours] = request.normHours?.toBigDecimal()
        }
        updatedRows > 0
    }

    fun delete(id: Long): Boolean = transaction {
        val deletedRows = ServicesTable.deleteWhere { ServicesTable.id eq id }
        deletedRows > 0
    }

    private fun rowToServiceResponse(row: ResultRow): ServiceResponse {
        return ServiceResponse(
            id = row[ServicesTable.id],
            name = row[ServicesTable.name],
            description = row[ServicesTable.description],
            basePrice = row[ServicesTable.basePrice].toPlainString(),
            normHours = row[ServicesTable.normHours]?.toPlainString()
        )
    }
}