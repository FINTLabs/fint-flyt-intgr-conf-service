package no.novari.flyt.catalog.discovery.mapping

import no.novari.flyt.catalog.discovery.model.dtos.InstanceObjectCollectionMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceObjectCollectionMetadata
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class InstanceObjectCollectionMetadataMappingService(
    @param:Lazy private val instanceMetadataContentMappingService: InstanceMetadataContentMappingService,
) {
    fun toEntity(
        instanceObjectCollectionMetadataDto: InstanceObjectCollectionMetadataDto,
    ): InstanceObjectCollectionMetadata =
        InstanceObjectCollectionMetadata(
            displayName = requireNotNull(instanceObjectCollectionMetadataDto.displayName),
            objectMetadata =
                instanceMetadataContentMappingService.toEntity(
                    requireNotNull(instanceObjectCollectionMetadataDto.objectMetadata),
                ),
            key = requireNotNull(instanceObjectCollectionMetadataDto.key),
        )

    fun toDto(instanceObjectCollectionMetadata: InstanceObjectCollectionMetadata): InstanceObjectCollectionMetadataDto =
        InstanceObjectCollectionMetadataDto(
            displayName = requireNotNull(instanceObjectCollectionMetadata.displayName),
            objectMetadata =
                instanceMetadataContentMappingService.toDto(
                    requireNotNull(instanceObjectCollectionMetadata.objectMetadata),
                ),
            key = requireNotNull(instanceObjectCollectionMetadata.key),
        )
}
