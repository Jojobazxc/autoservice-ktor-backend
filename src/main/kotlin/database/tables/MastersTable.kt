package com.example.database.tables

import com.example.common.enums.EmploymentStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.or

object MastersTable : Table("masters") {
    val id = long("id").autoIncrement()
    val fullName = varchar("full_name", 255)
    val specialization = varchar("specialization", 255).nullable()
    val experienceYears = integer("experience_years").nullable()
    val phone = varchar("phone", 20).nullable()
    val email = varchar("email", 255).nullable()
    val employmentStatus = enumerationByName("employment_status", 20, EmploymentStatus::class)

    override val primaryKey = PrimaryKey(id)

    init {
        check("masters_experience_years_check") {
            experienceYears.isNull() or (experienceYears greaterEq 0)
        }
    }
}