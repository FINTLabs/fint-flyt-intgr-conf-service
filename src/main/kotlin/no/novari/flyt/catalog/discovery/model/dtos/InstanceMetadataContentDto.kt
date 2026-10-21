package no.novari.flyt.catalog.discovery.model.dtos

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "Metadata describing values, object collections, and categories in a Flyt instance.")
data class InstanceMetadataContentDto(
    @field:Valid
    val instanceValueMetadata: List<@Valid InstanceValueMetadataDto> = emptyList(),
    @field:Valid
    val instanceObjectCollectionMetadata: List<@Valid InstanceObjectCollectionMetadataDto> = emptyList(),
    @field:Valid
    val categories: List<@Valid InstanceMetadataCategoryDto> = emptyList(),
)
