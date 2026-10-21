package no.novari.flyt.catalog.integration.api

import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPatchDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.application.IntegrationUpdateValidationService
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.catalog.integration.web.ConflictException
import no.novari.flyt.catalog.integration.web.ForbiddenWithoutBodyException
import no.novari.flyt.catalog.integration.web.NotFoundException
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.Authentication

@ExtendWith(MockitoExtension::class)
class IntegrationControllerTest {
    @Mock
    private lateinit var integrationService: IntegrationService

    @Mock
    private lateinit var integrationUpdateValidationService: IntegrationUpdateValidationService

    @Mock
    private lateinit var userAuthorizationService: UserAuthorizationService

    @Mock
    private lateinit var authentication: Authentication

    private lateinit var controller: IntegrationController

    @BeforeEach
    fun setUp() {
        controller =
            IntegrationController(
                integrationService,
                integrationUpdateValidationService,
                userAuthorizationService,
            )
    }

    @Test
    fun `returns integrations for user's authorized source applications`() {
        val candidateSourceApplicationIds = setOf(1L, 2L, 3L)
        val authorizedSourceApplicationIds = setOf(1L, 2L)
        val expectedIntegrations =
            listOf(
                IntegrationDto(
                    id = 1L,
                    sourceApplicationId = 1L,
                    sourceApplicationIntegrationId = "integration-1",
                    destination = "Destination 1",
                    state = Integration.State.ACTIVE,
                ),
                IntegrationDto(
                    id = 2L,
                    sourceApplicationId = 2L,
                    sourceApplicationIntegrationId = "integration-2",
                    destination = "Destination 2",
                    state = Integration.State.DEACTIVATED,
                ),
            )

        whenever(integrationService.findDistinctSourceApplicationIds()).thenReturn(candidateSourceApplicationIds)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                candidateSourceApplicationIds,
            ),
        ).thenReturn(authorizedSourceApplicationIds)
        whenever(integrationService.findAllBySourceApplicationIds(authorizedSourceApplicationIds))
            .thenReturn(expectedIntegrations)

        val response = controller.listIntegrations(authentication, null)

        assertEquals(expectedIntegrations.size, response.size)
        assertTrue(response.containsAll(expectedIntegrations))
        verify(integrationService).findDistinctSourceApplicationIds()
        verify(userAuthorizationService).getUserAuthorizedSourceApplicationIds(
            authentication,
            candidateSourceApplicationIds,
        )
        verify(integrationService).findAllBySourceApplicationIds(authorizedSourceApplicationIds)
        verify(integrationService, never()).findAll()
    }

    @Test
    fun `returns paginated integrations with totals`() {
        val candidateSourceApplicationIds = setOf(1L, 2L, 3L)
        val authorizedSourceApplicationIds = setOf(1L, 2L)
        val integration =
            IntegrationDto(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "Destination 1",
                state = Integration.State.ACTIVE,
            )
        val page = PageImpl(listOf(integration), PageRequest.of(0, 10), 1)

        whenever(integrationService.findDistinctSourceApplicationIds()).thenReturn(candidateSourceApplicationIds)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                candidateSourceApplicationIds,
            ),
        ).thenReturn(authorizedSourceApplicationIds)
        whenever(integrationService.findAllBySourceApplicationIds(eq(authorizedSourceApplicationIds), any()))
            .thenReturn(page)

        val response =
            controller.listIntegrationsPaginated(
                authentication = authentication,
                page = 0,
                size = 10,
                sortProperty = "id",
                sortDirection = Sort.Direction.ASC,
                sourceApplicationId = null,
            )

        assertEquals(listOf(integration), response.content)
        assertEquals(1L, response.totalElements)
        assertEquals(1, response.totalPages)
    }

    @Test
    fun `returns integration when found`() {
        val integration =
            IntegrationDto(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "destination",
                state = Integration.State.ACTIVE,
            )

        whenever(integrationService.findById(1L)).thenReturn(integration)

        val response = controller.getIntegrationById(authentication, 1L)

        assertEquals(integration, response)
        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
    }

    @Test
    fun `throws not found when integration is missing`() {
        whenever(integrationService.findById(1L)).thenReturn(null)

        val exception =
            assertThrows(NotFoundException::class.java) {
                controller.getIntegrationById(authentication, 1L)
            }

        assertTrue(exception.message?.contains("404") == true)
    }

    @Test
    fun `throws forbidden without body when filtering by unauthorized source application id`() {
        whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(authentication, setOf(3L)))
            .thenReturn(setOf(1L, 2L))

        assertThrows(ForbiddenWithoutBodyException::class.java) {
            controller.listIntegrations(authentication, 3L)
        }
    }

    @Test
    fun `creates integration when valid`() {
        val postDto = IntegrationPostDto(1L, "integration-1", "destination")
        val savedIntegration =
            IntegrationDto(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "destination",
                state = Integration.State.DEACTIVATED,
            )

        whenever(
            integrationService.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "integration-1",
            ),
        ).thenReturn(false)
        whenever(integrationService.save(postDto)).thenReturn(savedIntegration)

        val response = controller.createIntegration(authentication, postDto)

        assertEquals(savedIntegration, response)
        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
    }

    @Test
    fun `throws conflict when posting duplicate integration`() {
        val postDto = IntegrationPostDto(1L, "integration-1", "destination")

        whenever(
            integrationService.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "integration-1",
            ),
        ).thenReturn(true)

        val exception =
            assertThrows(ConflictException::class.java) {
                controller.createIntegration(authentication, postDto)
            }

        assertTrue(exception.message?.contains("409") == true)
    }

    @Test
    fun `delegates patch validation to update validation service`() {
        val existingIntegration =
            IntegrationDto(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "integration-1",
                destination = "destination",
                state = Integration.State.DEACTIVATED,
            )
        val patchDto =
            IntegrationPatchDto(
                destination = "updated-destination",
                state = Integration.State.ACTIVE,
                activeConfigurationId = 42L,
            )
        val updatedIntegration =
            existingIntegration.copy(
                destination = "updated-destination",
                state = Integration.State.ACTIVE,
                activeConfigurationId = 42L,
            )

        whenever(integrationService.findById(1L)).thenReturn(existingIntegration)
        whenever(integrationService.updateById(1L, patchDto)).thenReturn(updatedIntegration)

        val response = controller.updateIntegration(authentication, 1L, patchDto)

        assertEquals(updatedIntegration, response)
        verify(integrationUpdateValidationService).validate(1L, updatedIntegration)
    }
}
