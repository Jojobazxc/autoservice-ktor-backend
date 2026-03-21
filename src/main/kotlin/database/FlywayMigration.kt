package com.example.database

import javax.sql.DataSource
import org.flywaydb.core.Flyway

object FlywayFactory {

    fun migrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(false)
            .load()
            .migrate()
    }
}