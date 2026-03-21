package com.example.features.masters

import com.example.database.tables.MastersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class MasterRepository {

    fun getAll(): List<MasterResponse> = transaction {
        MastersTable
            .selectAll()
            .map(::rowToMasterResponse)
    }

    fun getById(id: Long): MasterResponse? = transaction {
        MastersTable
            .selectAll()
            .where { MastersTable.id eq id }
            .map(::rowToMasterResponse)
            .singleOrNull()
    }

    fun create(request: CreateMasterRequest): MasterResponse = transaction {
        val createdId = MastersTable.insert {
            it[fullName] = request.fullName
            it[specialization] = request.specialization
            it[experienceYears] = request.experienceYears
            it[phone] = request.phone
            it[email] = request.email
            it[employmentStatus] = request.employmentStatus
        }[MastersTable.id]

        MastersTable
            .selectAll()
            .where { MastersTable.id eq createdId }
            .map(::rowToMasterResponse)
            .single()
    }

    fun update(id: Long, request: UpdateMasterRequest): Boolean = transaction {
        val updatedRows = MastersTable.update({ MastersTable.id eq id }) {
            it[fullName] = request.fullName
            it[specialization] = request.specialization
            it[experienceYears] = request.experienceYears
            it[phone] = request.phone
            it[email] = request.email
            it[employmentStatus] = request.employmentStatus
        }
        updatedRows > 0
    }

    fun delete(id: Long): Boolean = transaction {
        val deletedRows = MastersTable.deleteWhere { MastersTable.id eq id }
        deletedRows > 0
    }

    private fun rowToMasterResponse(row: ResultRow): MasterResponse {
        return MasterResponse(
            id = row[MastersTable.id],
            fullName = row[MastersTable.fullName],
            specialization = row[MastersTable.specialization],
            experienceYears = row[MastersTable.experienceYears],
            phone = row[MastersTable.phone],
            email = row[MastersTable.email],
            employmentStatus = row[MastersTable.employmentStatus]
        )
    }
}