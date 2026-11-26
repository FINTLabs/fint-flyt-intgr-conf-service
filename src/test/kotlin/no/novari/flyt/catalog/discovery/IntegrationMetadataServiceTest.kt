package no.novari.flyt.catalog.discovery

import no.novari.flyt.catalog.discovery.mapping.IntegrationMetadataMappingService
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class IntegrationMetadataServiceTest {
    @Mock
    private lateinit var integrationMetadataRepository: IntegrationMetadataRepository

    @Mock
    private lateinit var integrationMetadataMappingService: IntegrationMetadataMappingService

    @InjectMocks
    private lateinit var service: IntegrationMetadataService

    private val sourceApplicationId = 1L
    private val dto = validIntegrationMetadataDto()
    private val entity =
        IntegrationMetadata(
            sourceApplicationId = sourceApplicationId,
            sourceApplicationIntegrationId = "integrationId",
            version = 1L,
        )

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `gets integration metadata for a source application with only latest versions`() {
        whenever(integrationMetadataRepository.findAllWithLatestVersionsForSourceApplication(sourceApplicationId))
            .thenReturn(listOf(entity))
        whenever(integrationMetadataMappingService.toDtos(listOf(entity))).thenReturn(listOf(dto))

        val result = service.getIntegrationMetadataForSourceApplication(sourceApplicationId, true)

        assertEquals(1, result.size)
        assertEquals(dto, result.first())
    }

    @Test
    fun `gets all integration metadata for a source application`() {
        whenever(integrationMetadataRepository.findAllBySourceApplicationId(sourceApplicationId))
            .thenReturn(listOf(entity))
        whenever(integrationMetadataMappingService.toDtos(listOf(entity))).thenReturn(listOf(dto))

        val result = service.getIntegrationMetadataForSourceApplication(sourceApplicationId, false)

        assertEquals(1, result.size)
        assertEquals(dto, result.first())
    }

    @Test
    fun `gets integration metadata for multiple source applications with only latest versions`() {
        val sourceApplicationIds = listOf(1L, 2L)
        whenever(integrationMetadataRepository.findAllWithLatestVersionsForSourceApplications(sourceApplicationIds))
            .thenReturn(listOf(entity))
        whenever(integrationMetadataMappingService.toDtos(listOf(entity))).thenReturn(listOf(dto))

        val result = service.getIntegrationMetadataForSourceApplications(sourceApplicationIds, true)

        assertEquals(2, result.size)
        assertEquals(1, result.getValue(1L).size)
        assertEquals(dto, result.getValue(1L).first())
        assertEquals(0, result.getValue(2L).size)
    }

    @Test
    fun `gets all integration metadata for multiple source applications`() {
        val sourceApplicationIds = listOf(1L, 2L)
        whenever(integrationMetadataRepository.findAllBySourceApplicationIdIn(sourceApplicationIds))
            .thenReturn(listOf(entity))
        whenever(integrationMetadataMappingService.toDtos(listOf(entity))).thenReturn(listOf(dto))

        val result = service.getIntegrationMetadataForSourceApplications(sourceApplicationIds, false)

        assertEquals(2, result.size)
        assertEquals(1, result.getValue(1L).size)
        assertEquals(dto, result.getValue(1L).first())
        assertEquals(0, result.getValue(2L).size)
    }

    @Test
    fun `returns empty map for empty source application input`() {
        val result = service.getIntegrationMetadataForSourceApplications(emptyList(), false)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `gets all integration metadata for a source application and integration id`() {
        val integrationId = "integrationId"
        whenever(
            integrationMetadataRepository.findAllBySourceApplicationIdAndSourceApplicationIntegrationId(
                sourceApplicationId,
                integrationId,
            ),
        ).thenReturn(listOf(entity))
        whenever(integrationMetadataMappingService.toDtos(listOf(entity))).thenReturn(listOf(dto))

        val result =
            service.getAllForSourceApplicationIdAndSourceApplicationIntegrationId(
                sourceApplicationId,
                integrationId,
            )

        assertEquals(1, result.size)
        assertEquals(dto, result.first())
    }

    @Test
    fun `gets integration metadata by id`() {
        whenever(integrationMetadataRepository.findById(1L)).thenReturn(java.util.Optional.of(entity))
        whenever(integrationMetadataMappingService.toDto(entity)).thenReturn(dto)

        val result = service.getById(1L)

        assertEquals(dto, result)
    }

    @Test
    fun `checks whether a version already exists`() {
        whenever(
            integrationMetadataRepository.existsBySourceApplicationIdAndSourceApplicationIntegrationIdAndVersion(
                sourceApplicationId,
                "integrationId",
                1L,
            ),
        ).thenReturn(true)

        val result = service.versionExists(dto)

        assertTrue(result)
    }

    @Test
    fun `saves integration metadata`() {
        val savedDto = validIntegrationMetadataDto(version = 2L)
        whenever(integrationMetadataMappingService.toEntity(dto)).thenReturn(entity)
        whenever(integrationMetadataRepository.save(entity)).thenReturn(entity)
        whenever(integrationMetadataMappingService.toDto(entity)).thenReturn(savedDto)

        val result = service.save(dto)

        assertEquals(savedDto, result)
        verify(integrationMetadataRepository).save(entity)
    }

    @Test
    fun `reports conflict when a version was registered concurrently`() {
        whenever(
            integrationMetadataRepository.existsBySourceApplicationIdAndSourceApplicationIntegrationIdAndVersion(
                sourceApplicationId,
                "integrationId",
                1L,
            ),
        ).thenReturn(false, true)
        whenever(integrationMetadataMappingService.toEntity(dto)).thenReturn(entity)
        whenever(integrationMetadataRepository.saveAndFlush(entity))
            .thenThrow(DataIntegrityViolationException("Duplicate version"))

        val exception = assertThrows<ResponseStatusException> { service.register(dto) }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertEquals("Metadata version already exists", exception.reason)
    }

    @Test
    fun `preserves a storage error when the version still does not exist`() {
        val storageError = DataIntegrityViolationException("Storage constraint violated")
        whenever(
            integrationMetadataRepository.existsBySourceApplicationIdAndSourceApplicationIntegrationIdAndVersion(
                sourceApplicationId,
                "integrationId",
                1L,
            ),
        ).thenReturn(false)
        whenever(integrationMetadataMappingService.toEntity(dto)).thenReturn(entity)
        whenever(integrationMetadataRepository.saveAndFlush(entity)).thenThrow(storageError)

        val exception = assertThrows<DataIntegrityViolationException> { service.register(dto) }

        assertSame(storageError, exception)
    }

    private fun validIntegrationMetadataDto(
        version: Long = 1L,
        instanceMetadata: InstanceMetadataContentDto? = InstanceMetadataContentDto(),
    ): IntegrationMetadataDto =
        IntegrationMetadataDto(
            sourceApplicationId = sourceApplicationId,
            sourceApplicationIntegrationId = "integrationId",
            sourceApplicationIntegrationUri = "uri",
            integrationDisplayName = "displayName",
            version = version,
            instanceMetadata = instanceMetadata,
        )
}
