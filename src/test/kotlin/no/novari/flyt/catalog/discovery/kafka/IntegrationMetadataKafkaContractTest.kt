package no.novari.flyt.catalog.discovery.kafka

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.KafkaPayloadFixtureRunner
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataCategoryDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceObjectCollectionMetadataDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceValueMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceMetadataContent
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Fastholder payloaden på `event.integration-metadata-received`, som acos-gateway og gateways bygget
 * på gateway-starter produserer.
 *
 * Hendelsen testes bare inn. `instanceMetadata` på entiteten er WRITE_ONLY, så hendelsen kan leses
 * inn men aldri skrives ut, og en round-trip ville feilet på et felt som fungerer helt som den skal
 * i drift.
 */
class IntegrationMetadataKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

    /**
     * Sammenligningen er rekursiv, ikke via equals: IntegrationMetadata sammenligner bare på id, så
     * isEqualTo ville passert uansett hva de øvrige feltene inneholdt. Hele treet må med - det er
     * her BOOLEAN og displayName fastholdes.
     */
    @Test
    fun `hendelsen bærer hele treet inn, ikke bare idempotensnøkkelen`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/event/integration-metadata-received")

        val received = runner.deserialize<IntegrationMetadata>(fixture)

        assertThat(received)
            .usingRecursiveComparison()
            .isEqualTo(receivedEventAsModel())
    }

    private fun receivedEventAsModel() =
        IntegrationMetadata(
            sourceApplicationId = 1L,
            sourceApplicationIntegrationId = "kildeapp-integrasjon",
            sourceApplicationIntegrationUri = "https://kildeapp.example/integrasjon/1",
            integrationDisplayName = "Byggesak",
            version = 1L,
            instanceMetadata =
                InstanceMetadataContent(
                    instanceValueMetadata =
                        mutableListOf(
                            valueMetadata("Tittel", InstanceValueMetadata.Type.STRING, "tittel"),
                            valueMetadata("Er hastesak", InstanceValueMetadata.Type.BOOLEAN, "erHastesak"),
                        ),
                ),
        )

    private fun valueMetadata(
        displayName: String,
        type: InstanceValueMetadata.Type,
        key: String,
    ) = InstanceValueMetadata(displayName = displayName, type = type, key = key)
}
