package no.novari.flyt.catalog.configuration.kafka

import no.novari.flyt.catalog.configuration.model.integration.Integration
import no.novari.flyt.catalog.configuration.model.metadata.InstanceMetadataCategory
import no.novari.flyt.catalog.configuration.model.metadata.InstanceMetadataContent
import no.novari.flyt.catalog.configuration.model.metadata.InstanceObjectCollectionMetadata
import no.novari.flyt.catalog.configuration.model.metadata.InstanceValueMetadata
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.KafkaPayloadFixtureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Testene går bare én vei. Tjenesten deserialiserer disse svarene og serialiserer dem aldri ut
 * igjen, så en rundtur ville fastholdt en form ingen kontrakt krever.
 */
class ConsumedKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

    @Test
    fun `integrasjonssvaret leses med activeConfigurationId som tall`() {
        val fixture = CatalogContractFixtures.kafkaById("integration/reply/integration-by-id")

        val integration = runner.deserialize<Integration>(fixture)

        assertThat(integration).isEqualTo(
            Integration(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "kildeapp-integrasjon",
                destination = "arkiv",
                state = Integration.State.ACTIVE,
                activeConfigurationId = 100L,
            ),
        )
    }

    @Test
    fun `ukjent integrasjon gir tom payload`() {
        val fixture = CatalogContractFixtures.kafkaById("integration/reply/integration-by-id-not-found")

        assertThat(runner.deserialize<Integration>(fixture)).isNull()
    }

    @Test
    fun `instansmetadatasvaret leses med BOOLEAN og visningsnavn`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/reply/instance-metadata-by-metadata-id")

        val content = runner.deserialize<InstanceMetadataContent>(fixture)

        assertThat(content).isEqualTo(
            InstanceMetadataContent(
                instanceValueMetadata =
                    mutableListOf(
                        InstanceValueMetadata(
                            displayName = "Tittel",
                            key = "tittel",
                            type = InstanceValueMetadata.Type.STRING,
                        ),
                        InstanceValueMetadata(
                            displayName = "Er hastesak",
                            key = "erHastesak",
                            type = InstanceValueMetadata.Type.BOOLEAN,
                        ),
                    ),
                instanceObjectCollectionMetadata =
                    mutableListOf(
                        InstanceObjectCollectionMetadata(
                            displayName = "Dokumenter",
                            key = "dokumenter",
                            objectMetadata =
                                InstanceMetadataContent(
                                    instanceValueMetadata =
                                        mutableListOf(
                                            InstanceValueMetadata(
                                                displayName = "Filnavn",
                                                key = "filnavn",
                                                type = InstanceValueMetadata.Type.STRING,
                                            ),
                                        ),
                                ),
                        ),
                    ),
                categories =
                    mutableListOf(
                        InstanceMetadataCategory(
                            displayName = "Avsender",
                            content =
                                InstanceMetadataContent(
                                    instanceValueMetadata =
                                        mutableListOf(
                                            InstanceValueMetadata(
                                                displayName = "Navn",
                                                key = "navn",
                                                type = InstanceValueMetadata.Type.STRING,
                                            ),
                                        ),
                                ),
                        ),
                    ),
            ),
        )
    }

    @Test
    fun `ukjent instansmetadata gir tom payload`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/reply/instance-metadata-by-metadata-id-not-found")

        assertThat(runner.deserialize<InstanceMetadataContent>(fixture)).isNull()
    }
}
