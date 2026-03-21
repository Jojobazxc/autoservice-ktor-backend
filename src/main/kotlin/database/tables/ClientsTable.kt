package com.example.database.tables

import com.example.common.enums.ClientStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ClientsTable : Table("clients") {
    val id = long("id").autoIncrement()
    val fullName = varchar("full_name", 255)
    val phone = varchar("phone", 20).uniqueIndex()
    val email = varchar("email", 255).nullable()
    val address = varchar("address", 255).nullable()
    val registrationDate = datetime("registration_date")
    val status = enumerationByName("status", 20, ClientStatus::class)

    override val primaryKey = PrimaryKey(id)
}