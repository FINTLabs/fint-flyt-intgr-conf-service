package no.novari.flyt.catalog.integration.application

import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.catalog.integration.api.dto.IntegrationPatchDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.persistence.IntegrationRepository
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class IntegrationServiceTest {
    @Mock
    private lateinit var integrationRepository: IntegrationRepository

    private lateinit var integrationService: IntegrationService

    @BeforeEach
    fun setUp() {
        val actorDisplayResolver: ActorDisplayResolver = mock()
        integrationService = IntegrationService(integrationRepository, IntegrationMappingService(actorDisplayResolver))
    }

    @Test
    fun `finds all integrations`() {
        val integrations =
            listOf(
                Integration(
                    id = 1L,
                    sourceApplicationId = 1L,
                    sourceApplicationIntegrationId = "integration-1",
                    destination = "destination-1",
                    state = Integration.State.ACTIVE,
                ),
            )
        whenever(integrationRepository.findAll()).thenReturn(integrations)

        val result = integrationService.findAll()

        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun `finds all integrations with pageable`() {
        val integrations =
            PageImpl(
                listOf(
                    Integration(
                        id = 1L,
                        sourceApplicationId = 1L,
                        sourceApplicationIntegrationId = "integration-1",
                        destination = "destination-1",
                        state = Integration.State.ACTIVE,
                    ),
                ),
            )
        whenever(integrationRepository.findAll(any<Pageable>())).thenReturn(integrations)

        val result = integrationService.findAll(Pageable.unpaged())

        assertEquals(1, result.totalElements)
        assertEquals(1L, result.content.single().id)
    }

    @Test
    fun `checks if integration exists by id`() {
        whenever(integrationRepository.existsById(1L)).thenReturn(true)

        val result = integrationService.existsById(1L)

        assertTrue(result)
    }

    @Test
    fun `finds integration by id`() {
        val integration =
            Integration(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "destination-1",
                state = Integration.State.ACTIVE,
            )
        whenever(integrationRepository.findById(1L)).thenReturn(Optional.of(integration))

        val result = integrationService.findById(1L)

        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `returns null when integration by id is missing`() {
        whenever(integrationRepository.findById(1L)).thenReturn(Optional.empty())

        val result = integrationService.findById(1L)

        assertNull(result)
    }

    @Test
    fun `finds integration by source application id and source application integration id`() {
        val integration =
            Integration(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "destination-1",
                state = Integration.State.ACTIVE,
            )
        whenever(
            integrationRepository.findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "integration-1",
            ),
        ).thenReturn(integration)

        val result =
            integrationService.findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "integration-1",
            )

        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `checks if integration exists by source application and integration id`() {
        whenever(
            integrationRepository.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "integration-1",
            ),
        ).thenReturn(true)

        val result =
            integrationService.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "integration-1",
            )

        assertTrue(result)
    }

    @Test
    fun `finds active configuration id by integration id`() {
        val integration = Integration(activeConfigurationId = 1L)
        whenever(integrationRepository.findById(1L)).thenReturn(Optional.of(integration))

        val result = integrationService.findActiveConfigurationIdByIntegrationId(1L)

        assertEquals(1L, result)
    }

    @Test
    fun `saves integration`() {
        val integrationPostDto = IntegrationPostDto(1L, "integration-1", "destination")
        val integration =
            Integration(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "destination",
                state = Integration.State.DEACTIVATED,
            )
        whenever(integrationRepository.save(any<Integration>())).thenReturn(integration)

        val result = integrationService.save(integrationPostDto)

        assertEquals(1L, result.id)
        assertEquals("destination", result.destination)
    }

    @Test
    fun `updates integration by id`() {
        val integration =
            Integration(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "destination",
                state = Integration.State.DEACTIVATED,
            )
        val integrationPatchDto =
            IntegrationPatchDto(
                destination = "updated-destination",
                state = Integration.State.ACTIVE,
                activeConfigurationId = 42L,
            )

        whenever(integrationRepository.getReferenceById(1L)).thenReturn(integration)
        whenever(integrationRepository.save(integration)).thenReturn(integration)

        val result = integrationService.updateById(1L, integrationPatchDto)

        assertEquals("updated-destination", result.destination)
        assertEquals(Integration.State.ACTIVE, result.state)
        assertEquals(42L, result.activeConfigurationId)
        verify(integrationRepository).save(integration)
    }
}
