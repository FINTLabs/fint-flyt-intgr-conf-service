package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.audit.actor.ActorDisplayProperties
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

class ConfigurationMappingServiceAuditTest {
    private val oid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val configurationMappingService =
        ConfigurationMappingService(
            mock<ObjectMappingMappingService>(),
            ActorDisplayResolver(
                { oids -> oids.associateWith { "Ola Nordmann" } },
                ActorDisplayProperties(),
            ),
        )

    @Test
    fun `hydrates actor display names for createdBy and lastModifiedBy`() {
        val configuration = configuration(Actor.User(oid), Actor.User(oid), legacyName = null)

        val result = configurationMappingService.toDto(configuration, true)

        assertEquals("Ola Nordmann", result.createdBy)
        assertEquals("Ola Nordmann", result.lastModifiedBy)
        assertEquals(Actor.User(oid), result.createdByActor)
        assertEquals(Actor.User(oid), result.lastModifiedByActor)
    }

    @Test
    fun `prefers the legacy name for rows migrated from before auditing`() {
        val configuration = configuration(Actor.Unknown, Actor.Unknown, legacyName = "Kari Nordmann")

        val result = configurationMappingService.toDto(configuration, true)

        assertEquals("Kari Nordmann", result.lastModifiedBy)
        assertEquals(Actor.Unknown, result.lastModifiedByActor)
    }

    @Test
    fun `uses the current actor once a migrated row has been modified by a known user`() {
        val configuration = configuration(Actor.Unknown, Actor.User(oid), legacyName = "Kari Nordmann")

        val result = configurationMappingService.toDto(configuration, true)

        assertEquals("Ola Nordmann", result.lastModifiedBy)
        assertEquals(Actor.User(oid), result.lastModifiedByActor)
    }

    @Test
    fun `maps a snapshot with metadata only`() {
        val configuration =
            Configuration
                .builder()
                .id(42L)
                .integrationId(1L)
                .integrationMetadataId(2L)
                .version(3)
                .completed(true)
                .comment("kommentar")
                .build()

        val snapshot = configurationMappingService.toSnapshot(configuration)

        assertEquals(42L, snapshot.id)
        assertEquals(1L, snapshot.integrationId)
        assertEquals(2L, snapshot.integrationMetadataId)
        assertEquals(3, snapshot.version)
        assertEquals(true, snapshot.completed)
        assertEquals("kommentar", snapshot.comment)
    }

    private fun configuration(
        createdBy: Actor,
        lastModifiedBy: Actor,
        legacyName: String?,
    ): Configuration =
        Configuration(
            id = 1L,
            integrationId = 1L,
            integrationMetadataId = 2L,
            lastModifiedByLegacy = legacyName,
        ).apply {
            ReflectionTestUtils.setField(this, "createdBy", createdBy)
            ReflectionTestUtils.setField(this, "lastModifiedBy", lastModifiedBy)
        }
}
