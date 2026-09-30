package no.novari.flyt.catalog.discovery.mapping

import no.novari.flyt.catalog.discovery.model.dtos.InstanceValueMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import org.springframework.stereotype.Service

@Service
class InstanceValueMetadataMappingService {
    fun toEntity(instanceValueMetadataDto: InstanceValueMetadataDto): InstanceValueMetadata =
        InstanceValueMetadata(
            displayName = requireNotNull(instanceValueMetadataDto.displayName),
            type = requireNotNull(instanceValueMetadataDto.type),
            key = requireNotNull(instanceValueMetadataDto.key),
        )

    fun toDto(instanceValueMetadata: InstanceValueMetadata): InstanceValueMetadataDto =
        InstanceValueMetadataDto(
            displayName = requireNotNull(instanceValueMetadata.displayName),
            type = requireNotNull(instanceValueMetadata.type),
            key = requireNotNull(instanceValueMetadata.key),
        )
}
