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
 * Fastholder payloadene på `request.mapping.by.configuration-id`, som configuration-domenet betjener
 * for mapping-service.
 *
 * Hver rolle testes i den retningen tjenesten faktisk bruker den: requesten deserialiseres, svaret
 * serialiseres.
 */
class ConfigurationKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

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
}
