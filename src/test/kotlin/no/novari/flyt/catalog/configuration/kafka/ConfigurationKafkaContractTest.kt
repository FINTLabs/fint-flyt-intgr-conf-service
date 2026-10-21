package no.novari.flyt.catalog.configuration.kafka

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.configuration.model.configuration.dtos.CollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.FromCollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.KafkaPayloadFixtureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Fastholder payloadene på de to request/reply-kontraktene configuration-domenet betjener.
 *
 * `request.mapping.by.configuration-id` må bestå etter sammenslåingen - mapping-service er klient.
 * `request.configuration.by.configuration-id` opphører når integration- og configuration-domenene
 * havner i samme tjeneste, men er i bruk fram til da, og fastholdes derfor på lik linje.
 *
 * Hver rolle testes i den retningen tjenesten faktisk bruker den: requesten deserialiseres, svaret
 * serialiseres.
 */
class ConfigurationKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

    @Test
    fun `konfigurasjonsrequesten er en bar Long`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/request/configuration-by-id")

        assertThat(runner.deserialize<Long>(fixture)).isEqualTo(1L)
    }

    @Test
    fun `konfigurasjonssvaret er uten mapping`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/reply/configuration-by-id")

        runner.verifySerialization(fixture, configuration())
    }

    @Test
    fun `ukjent konfigurasjon gir tom payload`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/reply/configuration-by-id-not-found")

        runner.verifySerialization(fixture, null)
    }

    @Test
    fun `mappingrequesten er en bar Long`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/request/mapping-by-configuration-id")

        assertThat(runner.deserialize<Long>(fixture)).isEqualTo(1L)
    }

    @Test
    fun `mappingsvaret er mapping-treet alene`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/reply/mapping-by-configuration-id")

        runner.verifySerialization(fixture, mappingTree())
    }

    @Test
    fun `ukjent mapping gir tom payload`() {
        val fixture = CatalogContractFixtures.kafkaById("configuration/reply/mapping-by-configuration-id-not-found")

        runner.verifySerialization(fixture, null)
    }

    private fun configuration() =
        ConfigurationDto(
            id = 1L,
            integrationId = 10L,
            integrationMetadataId = 100L,
            completed = false,
            comment = "Kommentar",
            version = 1,
            mapping = null,
            createdAt = Instant.parse("2026-01-15T09:00:00Z"),
            createdBy = FIRST_ACTOR_OID.toString(),
            createdByActor = Actor.User(FIRST_ACTOR_OID),
            lastModifiedAt = Instant.parse("2026-02-20T13:30:00Z"),
            lastModifiedBy = SECOND_ACTOR_OID.toString(),
            lastModifiedByActor = Actor.User(SECOND_ACTOR_OID),
        )

    private fun mappingTree() =
        ObjectMappingDto(
            valueMappingPerKey =
                mutableMapOf(
                    "tittel" to ValueMappingDto(type = ValueMapping.Type.STRING, mappingString = "Sakstittel"),
                ),
            valueCollectionMappingPerKey =
                mutableMapOf(
                    "vedlegg" to
                        CollectionMappingDto(
                            elementMappings =
                                mutableListOf(
                                    ValueMappingDto(type = ValueMapping.Type.FILE, mappingString = "\$if{...}"),
                                ),
                            fromCollectionMappings =
                                mutableListOf(
                                    FromCollectionMappingDto(
                                        instanceCollectionReferencesOrdered = mutableListOf("dokumenter"),
                                        elementMapping =
                                            ValueMappingDto(
                                                type = ValueMapping.Type.DYNAMIC_STRING,
                                                mappingString = "\$if{dokument.navn}",
                                            ),
                                    ),
                                ),
                        ),
                ),
            objectMappingPerKey =
                mutableMapOf(
                    "avsender" to
                        ObjectMappingDto(
                            valueMappingPerKey =
                                mutableMapOf(
                                    "navn" to
                                        ValueMappingDto(
                                            type = ValueMapping.Type.STRING,
                                            mappingString = "Ola Nordmann",
                                        ),
                                ),
                        ),
                ),
        )

    private companion object {
        private val FIRST_ACTOR_OID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val SECOND_ACTOR_OID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    }
}
