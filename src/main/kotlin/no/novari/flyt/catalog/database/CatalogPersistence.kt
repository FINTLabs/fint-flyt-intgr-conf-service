package no.novari.flyt.catalog.database

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.hibernate.cfg.AvailableSettings
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

private const val FLYWAY_LOCK_RETRY_COUNT = 300

fun catalogDataSource(
    properties: DataSourceProperties,
    schema: String,
): HikariDataSource =
    properties
        .initializeDataSourceBuilder()
        .type(HikariDataSource::class.java)
        .build()
        .apply {
            this.schema = schema
            poolName = schema
        }

/**
 * Skjemaene finnes og er migrert fra før, så `migrate` validerer bare checksummene og
 * bekrefter ved oppstart at applikasjonen snakker med det skjemaet den tror.
 */
fun catalogFlyway(
    dataSource: DataSource,
    schema: String,
    migrations: String,
): Flyway =
    Flyway
        .configure()
        .dataSource(dataSource)
        .schemas(schema)
        .locations("classpath:db/migration/$migrations")
        .lockRetryCount(FLYWAY_LOCK_RETRY_COUNT)
        .load()

/**
 * `hikari.schema` setter `search_path` per sesjon, og det overlever ikke en tilkoblingspooler i
 * transaksjonsmodus: neste transaksjon kan gå på en serverforbindelse med en annen `search_path`.
 * Hibernate må derfor kvalifisere tabellnavnene med skjemaet selv.
 */
fun EntityManagerFactoryBuilder.catalogEntityManagerFactory(
    dataSource: DataSource,
    schema: String,
    persistenceUnit: String,
    packages: List<String>,
): LocalContainerEntityManagerFactoryBean =
    dataSource(dataSource)
        .packages(*packages.toTypedArray())
        .properties(mapOf(AvailableSettings.DEFAULT_SCHEMA to schema))
        .persistenceUnit(persistenceUnit)
        .build()
