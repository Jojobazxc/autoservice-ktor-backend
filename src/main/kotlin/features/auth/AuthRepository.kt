package com.example.features.auth

import com.example.database.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class AuthRepository {

    fun findActiveByLogin(login: String): AuthUser? = transaction {
        UsersTable
            .selectAll()
            .where { (UsersTable.login eq login) and (UsersTable.isActive eq true) }
            .map(::rowToAuthUser)
            .singleOrNull()
    }

    private fun rowToAuthUser(row: ResultRow): AuthUser {
        return AuthUser(
            id = row[UsersTable.id],
            login = row[UsersTable.login],
            passwordHash = row[UsersTable.passwordHash],
            role = row[UsersTable.role],
            masterId = row[UsersTable.masterId]
        )
    }
}
