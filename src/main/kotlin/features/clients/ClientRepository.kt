package com.example.features.clients

import com.example.database.tables.ClientsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class ClientRepository {

    fun getAll(): List<ClientResponse> = transaction {
        ClientsTable
            .selectAll()
            .map(::rowToClientResponse)
    }

    fun getById(id: Long): ClientResponse? = transaction {
        ClientsTable
            .selectAll()
            .where { ClientsTable.id eq id }
            .map(::rowToClientResponse)
            .singleOrNull()
    }

    fun create(request: CreateClientRequest): ClientResponse = transaction {
        val createdId = ClientsTable.insert {
            it[fullName] = request.fullName
            it[phone] = request.phone
            it[email] = request.email
            it[address] = request.address
            it[registrationDate] = LocalDateTime.now()
            it[status] = request.status
        }[ClientsTable.id]

        ClientsTable
            .selectAll()
            .where { ClientsTable.id eq createdId }
            .map(::rowToClientResponse)
            .single()
    }

    fun update(id: Long, request: UpdateClientRequest): Boolean = transaction {
        val updatedRows = ClientsTable.update({ ClientsTable.id eq id }) {
            it[fullName] = request.fullName
            it[phone] = request.phone
            it[email] = request.email
            it[address] = request.address
            it[status] = request.status
        }
        updatedRows > 0
    }

    fun delete(id: Long): Boolean = transaction {
        val deletedRows = ClientsTable.deleteWhere { ClientsTable.id eq id }
        deletedRows > 0
    }

    private fun rowToClientResponse(row: ResultRow): ClientResponse {
        return ClientResponse(
            id = row[ClientsTable.id],
            fullName = row[ClientsTable.fullName],
            phone = row[ClientsTable.phone],
            email = row[ClientsTable.email],
            address = row[ClientsTable.address],
            registrationDate = row[ClientsTable.registrationDate].toString(),
            status = row[ClientsTable.status]
        )
    }
}