package no.novari.flyt.catalog.discovery

import no.novari.flyt.catalog.discovery.mapping.IntegrationMetadataMappingService
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    fun `gets instance metadata by id`() {
        val id = 1L
        val instanceDto = InstanceMetadataContentDto()
        val dtoWithInstanceMetadata = validIntegrationMetadataDto(instanceMetadata = instanceDto)
        whenever(integrationMetadataRepository.findById(id)).thenReturn(java.util.Optional.of(entity))
        whenever(integrationMetadataMappingService.toDto(entity)).thenReturn(dtoWithInstanceMetadata)

        val result = service.getInstanceMetadataById(id)

        assertEquals(instanceDto, result)
    }

    @Test
    fun `returns null instance metadata when entity does not exist`() {
        whenever(integrationMetadataRepository.findById(1L)).thenReturn(java.util.Optional.empty())

        val result = service.getInstanceMetadataById(1L)

        assertNull(result)
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
