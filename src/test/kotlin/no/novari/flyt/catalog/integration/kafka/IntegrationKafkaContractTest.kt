package no.novari.flyt.catalog.integration.kafka

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.KafkaPayloadFixtureRunner
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.SourceApplicationIdAndSourceApplicationIntegrationIdDto
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Fastholder payloadene på de to request/reply-kontraktene integration-domenet betjener.
 *
 * `request.integration.by.source-application-id-and-source-application-integration-id` har ni
 * gateway-repoer som klienter, og `request.active-configuration-id.by.integration-id` betjener
 * mapping-service. Ingen av dem skal måtte bygges eller deployes på nytt.
 */
class IntegrationKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

    @Test
    fun `oppslag på kildeapplikasjon tar et objekt, ikke en bar verdi`() {
        val fixture = CatalogContractFixtures.kafkaById("integration/request/integration-by-source-application")

        val request = runner.verifyRoundTrip<SourceApplicationIdAndSourceApplicationIntegrationIdDto>(fixture)

        assertThat(request).isEqualTo(
            SourceApplicationIdAndSourceApplicationIntegrationIdDto(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "kildeapp-integrasjon",
            ),
        )
    }

    @Test
    fun `oppslag på kildeapplikasjon svarer med samme form som oppslag på id`() {
        val fixture = CatalogContractFixtures.kafkaById("integration/reply/integration-by-source-application")

        runner.verifySerialization(fixture, integration())
    }

    @Test
    fun `ukjent kildeapplikasjon gir tom payload`() {
        val fixture =
            CatalogContractFixtures.kafkaById("integration/reply/integration-by-source-application-not-found")

        runner.verifySerialization(fixture, null)
    }

    @Test
    fun `requesten om aktiv konfigurasjon er en bar Long`() {
        val fixture =
            CatalogContractFixtures.kafkaById("integration/request/active-configuration-id-by-integration-id")

        assertThat(runner.deserialize<Long>(fixture)).isEqualTo(1L)
    }

    @Test
    fun `svaret om aktiv konfigurasjon er en bar Long`() {
        val fixture = CatalogContractFixtures.kafkaById("integration/reply/active-configuration-id-by-integration-id")

        runner.verifySerialization(fixture, 100L)
    }

    @Test
    fun `ingen aktiv konfigurasjon gir tom payload`() {
        val fixture =
            CatalogContractFixtures.kafkaById(
                "integration/reply/active-configuration-id-by-integration-id-not-found",
            )

        runner.verifySerialization(fixture, null)
    }

    private fun integration() =
        IntegrationDto(
            id = 1L,
            sourceApplicationId = 1L,
            sourceApplicationIntegrationId = "kildeapp-integrasjon",
            destination = "arkiv",
            state = Integration.State.ACTIVE,
            activeConfigurationId = 100L,
            createdAt = Instant.parse("2026-01-15T09:00:00Z"),
            createdBy = FIRST_ACTOR_OID.toString(),
            createdByActor = Actor.User(FIRST_ACTOR_OID),
            lastModifiedAt = Instant.parse("2026-02-20T13:30:00Z"),
            lastModifiedBy = SECOND_ACTOR_OID.toString(),
            lastModifiedByActor = Actor.User(SECOND_ACTOR_OID),
        )

    private companion object {
        private val FIRST_ACTOR_OID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val SECOND_ACTOR_OID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    }
}
