package no.novari.flyt.catalog

import no.novari.flyt.catalog.database.CatalogSchemas
import no.novari.flyt.catalog.database.catalogFlyway
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.sql.DriverManager

/**
 * En database satt opp slik katalogtjenesten møter den i drift, i motsetning til [CatalogPostgres],
 * der én superbruker eier alt, skjemanavnene er uprefiksede og tjenesten kobler direkte til Postgres:
 *
 * - Databasebrukeren heter `{tenant}_fint_flyt_intgr_conf_service_db` og eier bare sitt eget, tomme
 *   skjema. Rollens standard `search_path` (`"$user", public`) treffer dermed det skjemaet.
 * - De fire domeneskjemaene har tenant-prefiks, ble migrert av de gamle tjenestene og eies av hver
 *   sin rolle. Katalogtjenesten har tilgang gjennom grants, ikke eierskap.
 * - Skjemaprefikset utledes fra brukernavnet.
 * - Tjenesten kobler via PgBouncer i transaksjonsmodus, med `prepareThreshold=0` slik URL-en i drift
 *   har. En serverforbindelse deles mellom klienter per transaksjon, så sesjonstilstand en klient
 *   setter — som `search_path` fra `hikari.schema` — følger ikke med til neste transaksjon.
 *   `server_reset_query_always` gjør tapet deterministisk; i drift arver transaksjonen i stedet
 *   det forrige klient etterlot på serverforbindelsen.
 */
object ProductionLikeDatabase {
    private const val DATABASE = "fint_flyt"
    private const val TENANT_PREFIX = "test_no_"
    private const val PASSWORD = "catalog"
    private const val PGBOUNCER_PORT = 6432

    val schemas = CatalogSchemas(TENANT_PREFIX)
    private val username = schemas.own

    private val legacyDomains =
        mapOf(
            schemas.integration to "integration",
            schemas.configuration to "configuration",
            schemas.valueConverting to "valueconverting",
            schemas.discovery to "discovery",
        )

    private val pgBouncer: GenericContainer<*>

    init {
        CatalogPostgres.superuserConnection().use { connection ->
            connection.createStatement().use { it.execute("CREATE DATABASE $DATABASE") }
        }
        executeAsSuperuser(
            "CREATE ROLE $username LOGIN PASSWORD '$PASSWORD'",
            "CREATE SCHEMA $username AUTHORIZATION $username",
        )
        legacyDomains.forEach { (schema, migrations) -> createLegacySchema(schema, migrations) }

        pgBouncer =
            GenericContainer<Nothing>("edoburu/pgbouncer:v1.25.2-p0").apply {
                withExposedPorts(PGBOUNCER_PORT)
                withEnv(
                    mapOf(
                        "DB_HOST" to CatalogPostgres.bridgeIpAddress,
                        "DB_NAME" to DATABASE,
                        "DB_USER" to username,
                        "DB_PASSWORD" to PASSWORD,
                        "AUTH_TYPE" to "scram-sha-256",
                        "LISTEN_PORT" to PGBOUNCER_PORT.toString(),
                        "POOL_MODE" to "transaction",
                        "SERVER_RESET_QUERY" to "DISCARD ALL",
                        "SERVER_RESET_QUERY_ALWAYS" to "1",
                    ),
                )
                waitingFor(Wait.forListeningPort())
                start()
            }
    }

    fun registerDataSourceProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") {
            val port = pgBouncer.getMappedPort(PGBOUNCER_PORT)
            "jdbc:postgresql://${pgBouncer.host}:$port/$DATABASE?prepareThreshold=0"
        }
        registry.add("spring.datasource.username") { username }
        registry.add("spring.datasource.password") { PASSWORD }
    }

    fun executeAsSuperuser(vararg statements: String) {
        val connection =
            DriverManager.getConnection(directJdbcUrl(), CatalogPostgres.username, CatalogPostgres.password)
        connection.use { it.createStatement().use { statement -> statements.forEach(statement::execute) } }
    }

    private fun createLegacySchema(
        schema: String,
        migrations: String,
    ) {
        executeAsSuperuser(
            "CREATE ROLE $schema LOGIN PASSWORD '$PASSWORD'",
            "CREATE SCHEMA $schema AUTHORIZATION $schema",
        )
        catalogFlyway(DriverManagerDataSource(directJdbcUrl(), schema, PASSWORD), schema, migrations).migrate()
        executeAsSuperuser(
            "GRANT USAGE ON SCHEMA $schema TO $username",
            "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA $schema TO $username",
            "GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA $schema TO $username",
        )
    }

    private fun directJdbcUrl(): String = CatalogPostgres.jdbcUrlFor(DATABASE)
}
