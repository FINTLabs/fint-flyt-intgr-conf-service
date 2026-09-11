package no.novari.flyt.catalog

import jakarta.persistence.EntityManagerFactory
import no.novari.flyt.catalog.database.CatalogSchemas
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

class ApplicationTest : CatalogIntegrationTest() {
    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var schemas: CatalogSchemas

    @ParameterizedTest
    @CsvSource(
        "ownSchemaDataSource,       own",
        "integrationDataSource,     integration",
        "configurationDataSource,   configuration",
        "valueConvertingDataSource, valueConverting",
        "discoveryDataSource,       discovery",
    )
    fun `each data source connects to its own schema`(
        beanName: String,
        schemaProperty: String,
    ) {
        context.getBean(beanName, DataSource::class.java).connection.use { connection ->
            assertThat(connection.isValid(1)).isTrue()
            assertThat(connection.schema).isEqualTo(schemaOf(schemaProperty))
        }
    }

    @ParameterizedTest
    @CsvSource(
        "integrationFlyway,     integration,     2",
        "configurationFlyway,   configuration,   6",
        "valueConvertingFlyway, valueConverting, 5",
        "discoveryFlyway,       discovery,       2",
    )
    fun `each flyway configuration validates its own schema without checksum mismatches`(
        beanName: String,
        schemaProperty: String,
        expectedMigrations: Int,
    ) {
        val flyway = context.getBean(beanName, Flyway::class.java)

        assertThat(flyway.validateWithResult().validationSuccessful).isTrue()
        assertThat(flyway.info().pending()).isEmpty()
        // Flyway skriver også en versjonsløs rad for skjemaopprettelsen.
        assertThat(flyway.info().applied().mapNotNull { it.version?.version })
            .hasSize(expectedMigrations)
        assertThat(flyway.configuration.schemas).containsExactly(schemaOf(schemaProperty))
        // flyway_schema_history.script er filnavnet, ikke stien. Det er det som gjør at radene de
        // gamle tjenestene skrev fortsatt matcher etter at filene flyttet til hver sin katalog.
        assertThat(flyway.info().applied().mapNotNull { it.script }).noneMatch { it.contains("/") }
    }

    /**
     * Testen over validerer mot historikk de samme filene nettopp skrev, og kan derfor ikke oppdage at
     * en kopiert migrering har kommet i utakt med originalen. I drift møter Flyway historikk de gamle
     * tjenestene skrev, og sammenligner `script` og `checksum` mot den. Begge feltene er derfor pinnet
     * mot verdiene fra de gamle repoene, ikke mot våre egne kopier.
     */
    @ParameterizedTest
    @CsvSource(
        "integrationFlyway,     integration",
        "configurationFlyway,   configuration",
        "valueConvertingFlyway, valueconverting",
        "discoveryFlyway,       discovery",
    )
    fun `the migrations still carry the checksums the old services recorded in production`(
        beanName: String,
        domain: String,
    ) {
        val flyway = context.getBean(beanName, Flyway::class.java)

        val actual =
            flyway
                .info()
                .all()
                .filter { it.version != null }
                .associate { it.script to it.checksum }

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(LEGACY_CHECKSUMS.getValue(domain))
    }

    @Test
    fun `only the two envers-audited persistence units register the revision entity`() {
        assertThat(entityNamesOf("configurationEntityManagerFactory")).contains(REVISION_ENTITY)
        assertThat(entityNamesOf("valueConvertingEntityManagerFactory")).contains(REVISION_ENTITY)
        assertThat(entityNamesOf("integrationEntityManagerFactory")).doesNotContain(REVISION_ENTITY)
        assertThat(entityNamesOf("discoveryEntityManagerFactory")).doesNotContain(REVISION_ENTITY)
    }

    @Test
    fun `the service's own empty schema is the primary persistence unit`() {
        assertThat(context.getBean(DataSource::class.java))
            .isSameAs(context.getBean("ownSchemaDataSource"))
        assertThat(context.getBean(EntityManagerFactory::class.java))
            .isSameAs(context.getBean("ownSchemaEntityManagerFactory"))
        assertThat(context.getBean(PlatformTransactionManager::class.java))
            .isSameAs(context.getBean("ownSchemaTransactionManager"))
    }

    @Test
    fun `the primary persistence unit has no entities, so an unqualified write cannot reach domain data`() {
        assertThat(entityNamesOf("ownSchemaEntityManagerFactory")).isEmpty()
    }

    @Test
    fun `the value converting entity belongs to exactly one persistence unit`() {
        assertThat(entityNamesOf("valueConvertingEntityManagerFactory")).contains(VALUE_CONVERSION_ENTITY)
        listOf(
            "ownSchemaEntityManagerFactory",
            "integrationEntityManagerFactory",
            "configurationEntityManagerFactory",
            "discoveryEntityManagerFactory",
        ).forEach { beanName ->
            assertThat(entityNamesOf(beanName))
                .describedAs(beanName)
                .doesNotContain(VALUE_CONVERSION_ENTITY)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "IntegrationMetadata",
        "InstanceMetadataContent",
        "InstanceMetadataCategory",
        "InstanceObjectCollectionMetadata",
        "InstanceValueMetadata",
    )
    fun `the discovery entities belong to exactly one persistence unit`(entityName: String) {
        assertThat(entityNamesOf("discoveryEntityManagerFactory")).contains(entityName)
        listOf(
            "ownSchemaEntityManagerFactory",
            "integrationEntityManagerFactory",
            "configurationEntityManagerFactory",
            "valueConvertingEntityManagerFactory",
        ).forEach { beanName ->
            assertThat(entityNamesOf(beanName))
                .describedAs(beanName)
                .doesNotContain(entityName)
        }
    }

    /**
     * Klientene — mapping-service, configuration-service, acos-gateway og gateways bygget på
     * flyt-gateway-starter — skal ikke bygges eller deployes på nytt. Topic-navnene er derfor
     * kontrakt, og de bygges av tjenestens egen konfigurasjon, ikke av noe klienten sender.
     */
    @ParameterizedTest
    @CsvSource(
        "valueConversionByIdRequestConsumer,       test-no.flyt.request.value-converting.by.value-converting-id",
        "metadataByMetadataIdRequestConsumer,      test-no.flyt.request.metadata.by.metadata-id",
        "instanceMetadataByMetadataIdRequestConsumer, test-no.flyt.request.instance-metadata.by.metadata-id",
        "integrationMetadataEventConsumer,         test-no.flyt.event.integration-metadata-received",
    )
    fun `each consumer listens on the topic its clients produce to`(
        beanName: String,
        topic: String,
    ) {
        val container = context.getBean(beanName, ConcurrentMessageListenerContainer::class.java)

        assertThat(container.containerProperties.topics).containsExactly(topic)
    }

    @Test
    fun `each envers-audited schema has its own revinfo sequence`() {
        listOf(schemas.configuration, schemas.valueConverting).forEach { schema ->
            context.getBean("configurationDataSource", DataSource::class.java).connection.use { connection ->
                connection
                    .prepareStatement(
                        "SELECT count(*) FROM information_schema.sequences WHERE sequence_schema = ? AND sequence_name = 'revinfo_seq'",
                    ).use { statement ->
                        statement.setString(1, schema)
                        statement.executeQuery().use { rows ->
                            rows.next()
                            assertThat(rows.getInt(1)).describedAs("revinfo_seq i %s", schema).isEqualTo(1)
                        }
                    }
            }
        }
    }

    private fun schemaOf(property: String): String =
        when (property) {
            "own" -> schemas.own
            "integration" -> schemas.integration
            "configuration" -> schemas.configuration
            "valueConverting" -> schemas.valueConverting
            "discovery" -> schemas.discovery
            else -> error("Ukjent skjema: $property")
        }

    private fun entityNamesOf(beanName: String): List<String> =
        context
            .getBean(beanName, EntityManagerFactory::class.java)
            .metamodel.entities
            .map { it.name }

    companion object {
        private const val REVISION_ENTITY = "ActorRevisionEntity"
        private const val VALUE_CONVERSION_ENTITY = "ValueConversion"

        private val LEGACY_CHECKSUMS: Map<String, Map<String, Int>> =
            ApplicationTest::class.java
                .getResourceAsStream("/legacy-flyway-checksums.txt")!!
                .bufferedReader()
                .readLines()
                .filterNot { it.isBlank() || it.startsWith("#") }
                .map { it.split("|") }
                .groupBy({ it[0] }, { it[1] to it[2].toInt() })
                .mapValues { (_, entries) -> entries.toMap() }
    }
}
