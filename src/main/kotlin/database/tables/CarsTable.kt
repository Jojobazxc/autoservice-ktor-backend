package com.example.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or

object CarsTable : Table("cars") {
    val id = long("id").autoIncrement()
    val clientId = reference("client_id", ClientsTable.id, onDelete = ReferenceOption.CASCADE)
    val brand = varchar("brand", 100)
    val model = varchar("model", 100)
    val year = integer("year").nullable()
    val plateNumber = varchar("plate_number", 20).uniqueIndex()
    val vin = varchar("vin", 50).nullable().uniqueIndex()
    val mileage = integer("mileage").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        check("cars_year_check") {
            year.isNull() or ((year greaterEq 1900) and (year lessEq 2100))
        }
        check("cars_mileage_check") {
            mileage.isNull() or (mileage greaterEq 0)
        }
    }
}