package no.novari.flyt.catalog.integration.kafka

import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.KafkaPayloadFixtureRunner
import no.novari.flyt.catalog.integration.api.dto.ConfigurationDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Testen går bare én vei. Tjenesten deserialiserer dette svaret og serialiserer det aldri ut igjen,
 * så en rundtur ville fastholdt en form ingen kontrakt krever.
 *
 * `id` og `version` står i payloaden, men er READ_ONLY i modellen og deserialiseres derfor ikke.
 */
class ConsumedKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

    @Test
    fun `konfigurasjonssvaret leses inn i konfigurasjonsmodellen`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/reply/configuration-by-id")

        val configuration = runner.deserialize<ConfigurationDto>(fixture)

        assertThat(configuration).isEqualTo(
            ConfigurationDto(
                integrationId = 10L,
                integrationMetadataId = 100L,
                completed = false,
                comment = "Kommentar",
            ),
        )
    }

    @Test
    fun `ukjent konfigurasjon gir tom payload`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/reply/configuration-by-id-not-found")

        assertThat(runner.deserialize<ConfigurationDto>(fixture)).isNull()
    }
}
