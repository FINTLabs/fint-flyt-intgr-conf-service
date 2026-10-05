package no.novari.flyt.catalog

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource

/**
 * Full applikasjonskontekst mot ekte Postgres og ekte Kafka. Kafka er ikke valgfritt: konsumentene
 * oppretter og endrer topics når bønnene bygges, altså under oppstart.
 *
 * Alle arvingene deler egenskapssett, og dermed også den bufrede konteksten.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1)
@TestPropertySource(
    properties = [
        // Testcontainers-brukeren følger ikke {tenant}_{applikasjonsnavn}_db, så prefikset kan ikke utledes.
        "novari.flyt.catalog.database.schema-prefix=",
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
        "novari.kafka.default-replicas=1",
        "novari.kafka.topic.org-id=test-no",
        "fint.flyt.authorization.sso.client-id=test-client",
        "fint.flyt.authorization.sso.client-secret=test-secret",
    ],
)
abstract class CatalogIntegrationTest {
    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun datasourceProperties(registry: DynamicPropertyRegistry) =
            CatalogPostgres.registerDataSourceProperties(registry)
    }
}
