package no.novari.flyt.catalog.discovery

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import no.novari.flyt.catalog.discovery.validation.ValidationErrorsFormattingService
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException

class IntegrationMetadataControllerTest {
    private lateinit var controller: IntegrationMetadataController

    @Mock
    private lateinit var integrationMetadataService: IntegrationMetadataService

    @Mock
    private lateinit var validator: Validator

    @Mock
    private lateinit var authentication: Authentication

    @Mock
    private lateinit var userAuthorizationService: UserAuthorizationService

    @Mock
    private lateinit var validationErrorsFormattingService: ValidationErrorsFormattingService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        controller =
            IntegrationMetadataController(
                integrationMetadataService = integrationMetadataService,
                validator = validator,
                validationErrorsFormattingService = validationErrorsFormattingService,
                userAuthorizationService = userAuthorizationService,
            )
    }

    @Test
    fun `returns bad request when source application lookup has conflicting parameters`() {
        val response: ResponseEntity<Collection<IntegrationMetadata>> =
            controller.getIntegrationMetadataForSourceApplication()

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `returns 200 when integration metadata is found for a source application`() {
        val dto = validIntegrationMetadataDto()
        whenever(integrationMetadataService.getIntegrationMetadataForSourceApplication(1L, false))
            .thenReturn(listOf(dto))

        val response = controller.getIntegrationMetadataForSourceApplication(authentication, 1L, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.size)
    }

    @Test
    fun `returns 200 when integration metadata is found for multiple source applications`() {
        val dto = validIntegrationMetadataDto()
        val sourceApplicationIds = listOf(1L, 2L)
        val requestedSourceApplicationIds = setOf(1L, 2L)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                requestedSourceApplicationIds,
            ),
        ).thenReturn(requestedSourceApplicationIds)
        whenever(integrationMetadataService.getIntegrationMetadataForSourceApplications(sourceApplicationIds, false))
            .thenReturn(
                mapOf(
                    1L to listOf(dto),
                    2L to emptyList(),
                ),
            )

        val response =
            controller.getIntegrationMetadataForSourceApplications(
                authentication,
                sourceApplicationIds,
                null,
            )

        verify(userAuthorizationService).getUserAuthorizedSourceApplicationIds(
            authentication,
            requestedSourceApplicationIds,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(2, response.body?.size)
        assertEquals(1, response.body?.get(1L)?.size)
        assertEquals(0, response.body?.get(2L)?.size)
    }

    @Test
    fun `throws forbidden when user lacks access to a source application`() {
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=1",
            ),
        ).whenever(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)

        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.getIntegrationMetadataForSourceApplication(authentication, 1L, null)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertEquals(
            "You do not have permission to access or modify data that is related to source application with id=1",
            exception.reason,
        )
    }

    @Test
    fun `throws forbidden when user lacks access to one of several source applications`() {
        val requestedSourceApplicationIds = setOf(1L, 2L)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                requestedSourceApplicationIds,
            ),
        ).thenReturn(setOf(1L))

        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.getIntegrationMetadataForSourceApplications(authentication, listOf(1L, 2L), null)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        verify(userAuthorizationService).getUserAuthorizedSourceApplicationIds(
            authentication,
            requestedSourceApplicationIds,
        )
    }

    @Test
    fun `returns 200 for integration lookup by application and integration id`() {
        val dto = validIntegrationMetadataDto()
        whenever(
            integrationMetadataService.getAllForSourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "integrationId",
            ),
        ).thenReturn(listOf(dto))

        val response = controller.getIntegrationMetadataForIntegration(authentication, 1L, "integrationId")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.size)
    }

    @Test
    fun `throws forbidden for integration lookup without access`() {
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=1",
            ),
        ).whenever(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)

        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.getIntegrationMetadataForIntegration(authentication, 1L, "integrationId")
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertEquals(
            "You do not have permission to access or modify data that is related to source application with id=1",
            exception.reason,
        )
    }

    @Test
    fun `returns 200 when instance metadata content is found`() {
        val instanceMetadataContentDto = InstanceMetadataContentDto()
        val integrationMetadataDto =
            validIntegrationMetadataDto(
                sourceApplicationId = 1L,
                instanceMetadata = instanceMetadataContentDto,
            )
        whenever(integrationMetadataService.getById(1L)).thenReturn(integrationMetadataDto)

        val response = controller.getInstanceElementMetadataForIntegrationMetadataWithId(authentication, 1L)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(instanceMetadataContentDto, response.body)
    }

    @Test
    fun `throws not found when instance metadata lookup uses an invalid id`() {
        whenever(integrationMetadataService.getById(1L)).thenReturn(null)

        assertThrows(ResponseStatusException::class.java) {
            controller.getInstanceElementMetadataForIntegrationMetadataWithId(authentication, 1L)
        }
    }

    @Test
    fun `throws forbidden when user lacks access to instance metadata content`() {
        val metadataId = 1L
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=1",
            ),
        ).whenever(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        whenever(
            integrationMetadataService.getById(metadataId),
        ).thenReturn(validIntegrationMetadataDto(sourceApplicationId = 1L))

        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.getInstanceElementMetadataForIntegrationMetadataWithId(authentication, metadataId)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertEquals(
            "You do not have permission to access or modify data that is related to source application with id=1",
            exception.reason,
        )
    }

    @Test
    fun `throws exception when post has validation errors`() {
        val dto = validIntegrationMetadataDto()

        @Suppress("UNCHECKED_CAST")
        val violation = mock<ConstraintViolation<IntegrationMetadataDto>>()
        whenever(validator.validate(dto)).thenReturn(setOf(violation))
        whenever(validationErrorsFormattingService.format(any<Set<ConstraintViolation<IntegrationMetadataDto>>>()))
            .thenReturn("Validation error")

        assertThrows(ResponseStatusException::class.java) {
            controller.post(authentication, dto)
        }
    }

    @Test
    fun `throws forbidden when user lacks access on post`() {
        val integrationMetadataDto = validIntegrationMetadataDto(sourceApplicationId = 1L)
        whenever(validator.validate(integrationMetadataDto)).thenReturn(emptySet())
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=1",
            ),
        ).whenever(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)

        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.post(authentication, integrationMetadataDto)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertEquals(
            "You do not have permission to access or modify data that is related to source application with id=1",
            exception.reason,
        )
    }

    @Test
    fun `posts integration metadata successfully when there are no errors`() {
        val integrationMetadataDto = validIntegrationMetadataDto()
        whenever(validator.validate(integrationMetadataDto)).thenReturn(emptySet())
        whenever(integrationMetadataService.versionExists(integrationMetadataDto)).thenReturn(false)

        val response = controller.post(authentication, integrationMetadataDto)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(integrationMetadataService).save(integrationMetadataDto)
    }

    private fun validIntegrationMetadataDto(
        sourceApplicationId: Long = 1L,
        instanceMetadata: InstanceMetadataContentDto? = InstanceMetadataContentDto(),
    ): IntegrationMetadataDto =
        IntegrationMetadataDto(
            id = 1L,
            sourceApplicationId = sourceApplicationId,
            sourceApplicationIntegrationId = "integration-id",
            sourceApplicationIntegrationUri = "uri",
            integrationDisplayName = "displayName",
            version = 1L,
            instanceMetadata = instanceMetadata,
        )
}
