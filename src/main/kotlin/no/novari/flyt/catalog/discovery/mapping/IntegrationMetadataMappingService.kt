package no.novari.flyt.catalog.discovery.mapping

import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.springframework.stereotype.Service

@Service
class IntegrationMetadataMappingService(
    private val instanceMetadataContentMappingService: InstanceMetadataContentMappingService,
    private val actorDisplayResolver: ActorDisplayResolver,
) {
    fun toEntity(integrationMetadataDto: IntegrationMetadataDto): IntegrationMetadata =
        IntegrationMetadata(
            sourceApplicationId = requireNotNull(integrationMetadataDto.sourceApplicationId),
            sourceApplicationIntegrationId = requireNotNull(integrationMetadataDto.sourceApplicationIntegrationId),
            sourceApplicationIntegrationUri = integrationMetadataDto.sourceApplicationIntegrationUri,
            integrationDisplayName = requireNotNull(integrationMetadataDto.integrationDisplayName),
            version = requireNotNull(integrationMetadataDto.version),
            instanceMetadata =
                integrationMetadataDto.instanceMetadata?.let(instanceMetadataContentMappingService::toEntity),
        )

    fun toDto(integrationMetadata: IntegrationMetadata): IntegrationMetadataDto =
        toDto(integrationMetadata, actorDisplayResolver.resolve(integrationMetadata.createdBy))

    fun toDtos(integrationMetadataList: List<IntegrationMetadata>): List<IntegrationMetadataDto> {
        val displays = actorDisplayResolver.resolveAll(integrationMetadataList.map { it.createdBy })
        return integrationMetadataList.map { toDto(it, displays[it.createdBy]) }
    }

    private fun toDto(
        integrationMetadata: IntegrationMetadata,
        createdByDisplay: String?,
    ): IntegrationMetadataDto =
        IntegrationMetadataDto(
            id = integrationMetadata.id,
            sourceApplicationId = requireNotNull(integrationMetadata.sourceApplicationId),
            sourceApplicationIntegrationId = requireNotNull(integrationMetadata.sourceApplicationIntegrationId),
            sourceApplicationIntegrationUri = integrationMetadata.sourceApplicationIntegrationUri,
            integrationDisplayName = requireNotNull(integrationMetadata.integrationDisplayName),
            version = requireNotNull(integrationMetadata.version),
            instanceMetadata =
                integrationMetadata.instanceMetadata?.let(instanceMetadataContentMappingService::toDto),
            createdAt = integrationMetadata.createdAt,
            createdBy = createdByDisplay,
            createdByActor = integrationMetadata.createdBy,
        )
}
