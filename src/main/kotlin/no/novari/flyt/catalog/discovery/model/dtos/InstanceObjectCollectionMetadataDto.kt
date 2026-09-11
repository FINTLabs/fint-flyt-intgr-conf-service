package no.novari.flyt.catalog.discovery.model.dtos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class InstanceObjectCollectionMetadataDto(
    @field:NotBlank
    val displayName: String? = null,
    @field:NotNull
    @field:Valid
    var objectMetadata: InstanceMetadataContentDto? = null,
    @field:NotBlank
    val key: String? = null,
)
