package no.novari.flyt.catalog.integration.application

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.audit.entity.CreatedAuditedEntity
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

/**
 * createdAt/createdBy/lastModifiedAt/lastModifiedBy are only settable via the protected
 * Spring Data JPA auditing setters. Reflection mirrors what Hibernate does at persist/update time.
 */
private fun Integration.withAuditFields(
    createdAt: Instant,
    createdBy: Actor,
    lastModifiedAt: Instant,
    lastModifiedBy: Actor,
): Integration =
    apply {
        val createdAtField =
            CreatedAuditedEntity::class.java.getDeclaredField("createdAt").apply { isAccessible = true }
        val createdByField =
            CreatedAuditedEntity::class.java.getDeclaredField("createdBy").apply { isAccessible = true }
        val lastModifiedAtField =
            javaClass.superclass.getDeclaredField("lastModifiedAt").apply { isAccessible = true }
        val lastModifiedByField =
            javaClass.superclass.getDeclaredField("lastModifiedBy").apply { isAccessible = true }
        createdAtField.set(this, createdAt)
        createdByField.set(this, createdBy)
        lastModifiedAtField.set(this, lastModifiedAt)
        lastModifiedByField.set(this, lastModifiedBy)
    }

class IntegrationMappingServiceTest {
    private lateinit var actorDisplayResolver: ActorDisplayResolver
    private lateinit var integrationMappingService: IntegrationMappingService

    @BeforeEach
    fun setUp() {
        actorDisplayResolver = mock()
        whenever(actorDisplayResolver.resolveAll(any())).thenReturn(emptyMap())
        integrationMappingService = IntegrationMappingService(actorDisplayResolver)
    }

    @Test
    fun `maps post dto to integration`() {
        val integrationPostDto =
            IntegrationPostDto(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "Test",
                destination = "Destination",
            )

        val integration = integrationMappingService.toIntegration(integrationPostDto)

        assertNotNull(integration)
        assertEquals(integrationPostDto.sourceApplicationId, integration.sourceApplicationId)
        assertEquals(
            integrationPostDto.sourceApplicationIntegrationId,
            integration.sourceApplicationIntegrationId,
        )
        assertEquals(integrationPostDto.destination, integration.destination)
        assertEquals(Integration.State.DEACTIVATED, integration.state)
    }

    @Test
    fun `maps integration to dto`() {
        val integration =
            Integration(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "Test",
                destination = "Destination",
                state = Integration.State.ACTIVE,
                activeConfigurationId = 2L,
            )

        val integrationDto = integrationMappingService.toDto(integration)

        assertNotNull(integrationDto)
        assertEquals(integration.id, integrationDto.id)
        assertEquals(integration.sourceApplicationId, integrationDto.sourceApplicationId)
        assertEquals(
            integration.sourceApplicationIntegrationId,
            integrationDto.sourceApplicationIntegrationId,
        )
        assertEquals(integration.destination, integrationDto.destination)
        assertEquals(integration.state, integrationDto.state)
        assertEquals(integration.activeConfigurationId, integrationDto.activeConfigurationId)
    }

    @Test
    fun `hydrates actor display names when mapping to dto`() {
        val createdBy = Actor.User(UUID.randomUUID())
        val lastModifiedBy = Actor.System
        val integration =
            Integration(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "Test",
                destination = "Destination",
                state = Integration.State.ACTIVE,
            ).withAuditFields(
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                createdBy = createdBy,
                lastModifiedAt = Instant.parse("2026-02-01T00:00:00Z"),
                lastModifiedBy = lastModifiedBy,
            )
        whenever(actorDisplayResolver.resolve(createdBy)).thenReturn("Ola Nordmann")
        whenever(actorDisplayResolver.resolve(lastModifiedBy)).thenReturn("System")

        val integrationDto = integrationMappingService.toDto(integration)

        assertEquals(integration.createdAt, integrationDto.createdAt)
        assertEquals("Ola Nordmann", integrationDto.createdBy)
        assertEquals(createdBy, integrationDto.createdByActor)
        assertEquals(integration.lastModifiedAt, integrationDto.lastModifiedAt)
        assertEquals("System", integrationDto.lastModifiedBy)
        assertEquals(lastModifiedBy, integrationDto.lastModifiedByActor)
    }

    @Test
    fun `maps integration collection to dtos`() {
        val integrations =
            listOf(
                Integration(
                    id = 1L,
                    sourceApplicationId = 1L,
                    sourceApplicationIntegrationId = "Test1",
                    destination = "Destination1",
                    state = Integration.State.ACTIVE,
                    activeConfigurationId = 2L,
                ),
                Integration(
                    id = 2L,
                    sourceApplicationId = 2L,
                    sourceApplicationIntegrationId = "Test2",
                    destination = "Destination2",
                    state = Integration.State.DEACTIVATED,
                    activeConfigurationId = 3L,
                ),
            )

        val integrationDtos = integrationMappingService.toDtos(integrations)

        assertNotNull(integrationDtos)
        assertEquals(integrations.size, integrationDtos.size)
    }
}
