package no.novari.flyt.catalog

import no.novari.flyt.catalog.database.CatalogSchemas
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

/**
 * Én container for hele testkjøringen. `@Container` starter én per testklasse, og hver oppstart
 * kjører alle fire domenenes Flyway-migreringer på nytt.
 */
object CatalogPostgres {
    private val container: PostgreSQLContainer<*> =
        PostgreSQLContainer<Nothing>("postgres:17-alpine").apply {
            start()
            createOwnSchema()
        }

    /**
     * `@ServiceConnection` bidrar med en `JdbcConnectionDetails`-bønne, ikke `spring.datasource.*`,
     * og de fem datakildene bygges fra `DataSourceProperties`.
     */
    fun registerDataSourceProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url", container::getJdbcUrl)
        registry.add("spring.datasource.username", container::getUsername)
        registry.add("spring.datasource.password", container::getPassword)
    }

    /**
     * I drift oppretter pgerator dette skjemaet sammen med databasebrukeren. Ingen
     * Flyway-konfigurasjon eier det, så testen må opprette det selv for å speile oppsettet
     * applikasjonen møter.
     */
    private fun PostgreSQLContainer<*>.createOwnSchema() {
        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            connection.createStatement().use {
                it.execute("CREATE SCHEMA IF NOT EXISTS ${CatalogSchemas.OWN_SCHEMA}")
            }
        }
    }
}
