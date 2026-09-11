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
 * Fastholder payloadene på discovery-domenets Kafka-kontrakter.
 *
 * De to request/reply-kontraktene opphører ved sammenslåingen - configuration-service er eneste
 * klient - men er i bruk fram til da. `event.integration-metadata-received` må derimot bestå:
 * acos-gateway og gateways bygget på gateway-starter produserer den.
 *
 * Hver kontrakt testes i den retningen tjenesten faktisk bruker den. Det er ikke kosmetikk her:
 * `instanceMetadata` på entiteten er WRITE_ONLY, så hendelsen kan leses inn men aldri skrives ut,
 * og en round-trip ville feilet på et felt som fungerer helt som den skal i drift.
 */
class IntegrationMetadataKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

    @Test
    fun `metadatarequesten er en bar Long`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/request/metadata-by-metadata-id")

        assertThat(runner.deserialize<Long>(fixture)).isEqualTo(1L)
    }

    @Test
    fun `metadatasvaret er uten instansmetadata`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/reply/metadata-by-metadata-id")

        runner.verifySerialization(fixture, storedMetadata())
    }

    @Test
    fun `ukjent metadata gir tom payload`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/reply/metadata-by-metadata-id-not-found")

        runner.verifySerialization(fixture, null)
    }

    @Test
    fun `instansmetadatarequesten er en bar Long`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/request/instance-metadata-by-metadata-id")

        assertThat(runner.deserialize<Long>(fixture)).isEqualTo(1L)
    }

    @Test
    fun `instansmetadatasvaret er treet med alle verditypene`() {
        val fixture = CatalogContractFixtures.kafkaById("discovery/reply/instance-metadata-by-metadata-id")

        runner.verifySerialization(fixture, instanceMetadataContent())
    }

    @Test
    fun `ukjent instansmetadata gir tom payload`() {
        val fixture =
            CatalogContractFixtures.kafkaById("discovery/reply/instance-metadata-by-metadata-id-not-found")

        runner.verifySerialization(fixture, null)
    }

    /**
     * Sammenligningen er rekursiv, ikke via equals: IntegrationMetadata sammenligner bare på id, så
     * isEqualTo ville passert uansett hva de øvrige feltene inneholdt. Hele treet må med - det er
     * her BOOLEAN og displayName fastholdes, og de er blant duplikatene FFS-2256 skal samordne.
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

    private fun storedMetadata() =
        IntegrationMetadata(
            id = 1L,
            sourceApplicationId = 1L,
            sourceApplicationIntegrationId = "kildeapp-integrasjon",
            sourceApplicationIntegrationUri = "https://kildeapp.example/integrasjon/1",
            integrationDisplayName = "Byggesak",
            version = 1L,
            instanceMetadata = null,
        ).withAuditFieldsAsIfLoadedFromDatabase()

    private fun instanceMetadataContent() =
        InstanceMetadataContentDto(
            instanceValueMetadata =
                listOf(
                    value("Tittel", InstanceValueMetadata.Type.STRING, "tittel"),
                    value("Er hastesak", InstanceValueMetadata.Type.BOOLEAN, "erHastesak"),
                ),
            instanceObjectCollectionMetadata =
                listOf(
                    InstanceObjectCollectionMetadataDto(
                        displayName = "Dokumenter",
                        key = "dokumenter",
                        objectMetadata =
                            InstanceMetadataContentDto(
                                instanceValueMetadata =
                                    listOf(value("Filnavn", InstanceValueMetadata.Type.STRING, "filnavn")),
                            ),
                    ),
                ),
            categories =
                listOf(
                    InstanceMetadataCategoryDto(
                        displayName = "Avsender",
                        content =
                            InstanceMetadataContentDto(
                                instanceValueMetadata =
                                    listOf(value("Navn", InstanceValueMetadata.Type.STRING, "navn")),
                            ),
                    ),
                ),
        )

    private fun value(
        displayName: String,
        type: InstanceValueMetadata.Type,
        key: String,
    ) = InstanceValueMetadataDto(displayName = displayName, type = type, key = key)

    /**
     * Audit-feltene har `protected set` og populeres av JPA-auditing, ikke av konstruktøren. Denne
     * testen kjører uten database, så feltene settes direkte - ellers ville payloaden hatt null der
     * en rad hentet fra databasen har verdier, og fixturen ville fastholdt feil form.
     */
    private fun IntegrationMetadata.withAuditFieldsAsIfLoadedFromDatabase() =
        apply {
            setInheritedField("createdAt", Instant.parse("2026-01-15T09:00:00Z"))
            setInheritedField("createdBy", Actor.User(ACTOR_OID))
        }

    private fun Any.setInheritedField(
        name: String,
        value: Any?,
    ) {
        generateSequence(javaClass) { it.superclass }
            .mapNotNull { type -> runCatching { type.getDeclaredField(name) }.getOrNull() }
            .firstOrNull()
            ?.also { field ->
                field.isAccessible = true
                field.set(this, value)
            }
            ?: error("Fant ikke feltet '$name' på ${javaClass.name} eller superklassene")
    }

    private companion object {
        private val ACTOR_OID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    }
}
