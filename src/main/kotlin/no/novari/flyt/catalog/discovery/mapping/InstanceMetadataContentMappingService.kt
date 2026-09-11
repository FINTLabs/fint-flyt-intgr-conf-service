package no.novari.flyt.catalog.discovery.mapping

import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceMetadataContent
import org.springframework.stereotype.Service

@Service
class InstanceMetadataContentMappingService(
    private val instanceValueMetadataMappingService: InstanceValueMetadataMappingService,
    private val instanceObjectCollectionMetadataMappingService: InstanceObjectCollectionMetadataMappingService,
    private val instanceMetadataCategoryMappingService: InstanceMetadataCategoryMappingService,
) {
    fun toEntity(instanceMetadataContentDto: InstanceMetadataContentDto): InstanceMetadataContent =
        InstanceMetadataContent(
            instanceValueMetadata =
                instanceMetadataContentDto.instanceValueMetadata
                    .map(instanceValueMetadataMappingService::toEntity)
                    .toMutableList(),
            instanceObjectCollectionMetadata =
                instanceMetadataContentDto.instanceObjectCollectionMetadata
                    .map(
                        instanceObjectCollectionMetadataMappingService::toEntity,
                    ).toMutableList(),
            categories =
                instanceMetadataContentDto.categories
                    .map(instanceMetadataCategoryMappingService::toEntity)
                    .toMutableList(),
        )

    fun toDto(instanceMetadataContent: InstanceMetadataContent): InstanceMetadataContentDto =
        InstanceMetadataContentDto(
            instanceValueMetadata =
                instanceMetadataContent.instanceValueMetadata.map(instanceValueMetadataMappingService::toDto),
            instanceObjectCollectionMetadata =
                instanceMetadataContent.instanceObjectCollectionMetadata.map(
                    instanceObjectCollectionMetadataMappingService::toDto,
                ),
            categories = instanceMetadataContent.categories.map(instanceMetadataCategoryMappingService::toDto),
        )
}
