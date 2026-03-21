package com.example.features.parts

import com.example.database.tables.PartsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class PartRepository {

    fun getAll(): List<PartResponse> = transaction {
        PartsTable
            .selectAll()
            .map(::rowToPartResponse)
    }

    fun getById(id: Long): PartResponse? = transaction {
        PartsTable
            .selectAll()
            .where { PartsTable.id eq id }
            .map(::rowToPartResponse)
            .singleOrNull()
    }

    fun create(request: CreatePartRequest): PartResponse = transaction {
        val createdId = PartsTable.insert {
            it[name] = request.name
            it[article] = request.article
            it[price] = request.price.toBigDecimal()
            it[unit] = request.unit
            it[stockQuantity] = request.stockQuantity
        }[PartsTable.id]

        PartsTable
            .selectAll()
            .where { PartsTable.id eq createdId }
            .map(::rowToPartResponse)
            .single()
    }

    fun update(id: Long, request: UpdatePartRequest): Boolean = transaction {
        val updatedRows = PartsTable.update({ PartsTable.id eq id }) {
            it[name] = request.name
            it[article] = request.article
            it[price] = request.price.toBigDecimal()
            it[unit] = request.unit
            it[stockQuantity] = request.stockQuantity
        }
        updatedRows > 0
    }

    fun delete(id: Long): Boolean = transaction {
        val deletedRows = PartsTable.deleteWhere { PartsTable.id eq id }
        deletedRows > 0
    }

    private fun rowToPartResponse(row: ResultRow): PartResponse {
        return PartResponse(
            id = row[PartsTable.id],
            name = row[PartsTable.name],
            article = row[PartsTable.article],
            price = row[PartsTable.price].toPlainString(),
            unit = row[PartsTable.unit],
            stockQuantity = row[PartsTable.stockQuantity]
        )
    }
}