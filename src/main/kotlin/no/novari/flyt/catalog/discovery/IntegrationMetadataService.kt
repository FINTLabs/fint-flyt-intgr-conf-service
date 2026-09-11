package no.novari.flyt.catalog.discovery

import no.novari.flyt.catalog.discovery.mapping.IntegrationMetadataMappingService
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import org.springframework.stereotype.Service

@Service
class IntegrationMetadataService(
    private val integrationMetadataRepository: IntegrationMetadataRepository,
    private val integrationMetadataMappingService: IntegrationMetadataMappingService,
) {
    fun getIntegrationMetadataForSourceApplication(
        sourceApplicationId: Long,
        onlyLatestVersions: Boolean,
    ): List<IntegrationMetadataDto> {
        val entities =
            if (onlyLatestVersions) {
                integrationMetadataRepository.findAllWithLatestVersionsForSourceApplication(sourceApplicationId)
            } else {
                integrationMetadataRepository.findAllBySourceApplicationId(sourceApplicationId)
            }

        return integrationMetadataMappingService.toDtos(entities)
    }

    fun getIntegrationMetadataForSourceApplications(
        sourceApplicationIds: Collection<Long>,
        onlyLatestVersions: Boolean,
    ): Map<Long, Collection<IntegrationMetadataDto>> {
        if (sourceApplicationIds.isEmpty()) {
            return emptyMap()
        }

        val entities =
            if (onlyLatestVersions) {
                integrationMetadataRepository.findAllWithLatestVersionsForSourceApplications(sourceApplicationIds)
            } else {
                integrationMetadataRepository.findAllBySourceApplicationIdIn(sourceApplicationIds)
            }
        val grouped =
            integrationMetadataMappingService
                .toDtos(entities)
                .groupBy { requireNotNull(it.sourceApplicationId) }

        return sourceApplicationIds.associateWith { sourceApplicationId ->
            grouped[sourceApplicationId].orEmpty()
        }
    }

    fun getAllForSourceApplicationIdAndSourceApplicationIntegrationId(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
    ): List<IntegrationMetadataDto> =
        integrationMetadataMappingService.toDtos(
            integrationMetadataRepository.findAllBySourceApplicationIdAndSourceApplicationIntegrationId(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationIntegrationId = sourceApplicationIntegrationId,
            ),
        )

    fun getInstanceMetadataById(id: Long): InstanceMetadataContentDto? =
        integrationMetadataRepository
            .findById(id)
            .map(integrationMetadataMappingService::toDto)
            .map(IntegrationMetadataDto::instanceMetadata)
            .orElse(null)

    fun getById(id: Long): IntegrationMetadataDto? =
        integrationMetadataRepository
            .findById(id)
            .map(integrationMetadataMappingService::toDto)
            .orElse(null)

    fun versionExists(integrationMetadataDto: IntegrationMetadataDto): Boolean =
        integrationMetadataRepository.existsBySourceApplicationIdAndSourceApplicationIntegrationIdAndVersion(
            sourceApplicationId = requireNotNull(integrationMetadataDto.sourceApplicationId),
            sourceApplicationIntegrationId = requireNotNull(integrationMetadataDto.sourceApplicationIntegrationId),
            version = requireNotNull(integrationMetadataDto.version),
        )

    fun save(integrationMetadataDto: IntegrationMetadataDto): IntegrationMetadataDto =
        integrationMetadataMappingService.toDto(
            integrationMetadataRepository.save(
                integrationMetadataMappingService.toEntity(integrationMetadataDto),
            ),
        )
}
