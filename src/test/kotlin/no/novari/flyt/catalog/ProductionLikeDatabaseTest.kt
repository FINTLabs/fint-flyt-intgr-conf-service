package no.novari.flyt.catalog

import no.novari.flyt.catalog.database.CatalogSchemas
import no.novari.flyt.catalog.valueconverting.api.dto.ValueConversionRequest
import no.novari.flyt.catalog.valueconverting.application.ValueConversionService
import no.novari.flyt.catalog.valueconverting.domain.ValueConversionHistoryService
import no.novari.flyt.catalog.valueconverting.setAuthenticatedUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import javax.sql.DataSource

/**
 * De øvrige integrasjonstestene kobler direkte til Postgres som superbruker mot uprefiksede skjemaer,
 * og kan derfor ikke oppdage feil som bare viser seg med tilkoblingspooleren, databasebrukeren,
 * prefiksene og tilgangene i drift. Denne testen kjører mot [ProductionLikeDatabase].
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1)
@TestPropertySource(
    properties = [
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        "novari.kafka.default-replicas=1",
        "novari.kafka.topic.org-id=test-no",
        "fint.flyt.authorization.sso.client-id=test-client",
        "fint.flyt.authorization.sso.client-secret=test-secret",
    ],
)
class ProductionLikeDatabaseTest {
    @Autowired
    private lateinit var schemas: CatalogSchemas

    @Autowired
    private lateinit var valueConversionService: ValueConversionService

    @Autowired
    private lateinit var valueConversionHistoryService: ValueConversionHistoryService

    @Autowired
    @Qualifier("valueConvertingDataSource")
    private lateinit var valueConvertingDataSource: DataSource

    @Autowired
    @Qualifier("valueConvertingTransactionManager")
    private lateinit var valueConvertingTransactionManager: PlatformTransactionManager

    @AfterEach
    fun clearState() {
        SecurityContextHolder.clearContext()
        ProductionLikeDatabase.executeAsSuperuser(
            "SET search_path TO ${schemas.valueConverting}",
            "TRUNCATE converting_map_aud, value_converting_aud, converting_map, value_converting, revinfo",
        )
    }

    @Test
    fun `the schema prefix is derived from the database user`() {
        assertThat(schemas.valueConverting).isEqualTo(ProductionLikeDatabase.schemas.valueConverting)
    }

    @Test
    fun `value conversions are read and written in the value converting schema, with audit history`() {
        setAuthenticatedUser(UUID.fromString("11111111-1111-1111-1111-111111111111"))

        val created = valueConversionService.save(request(displayName = "Original"))
        val id = checkNotNull(created.id)
        valueConversionService.update(id, request(displayName = "Updated"))

        assertThat(valueConversionService.findDistinctSourceApplicationIds()).containsExactly(42L)
        assertThat(valueConversionService.findById(id)?.displayName).isEqualTo("Updated")
        val history =
            TransactionTemplate(valueConvertingTransactionManager).execute {
                valueConversionHistoryService.findHistory(id, PageRequest.of(0, 10)).content
            }
        assertThat(history).hasSize(2)
        assertThat(countRows("value_converting")).isEqualTo(1)
        assertThat(countRows("value_converting_aud")).isEqualTo(2)
    }

    private fun countRows(table: String): Int =
        JdbcTemplate(valueConvertingDataSource)
            .queryForObject<Int>("SELECT count(*) FROM ${schemas.valueConverting}.$table")!!

    private fun request(displayName: String) =
        ValueConversionRequest(
            displayName = displayName,
            fromApplicationId = 42L,
            fromTypeId = "fromType",
            toApplicationId = "toApp",
            toTypeId = "toType",
            convertingMap = mapOf("A" to "B"),
        )

    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun datasourceProperties(registry: DynamicPropertyRegistry) =
            ProductionLikeDatabase.registerDataSourceProperties(registry)
    }
}
