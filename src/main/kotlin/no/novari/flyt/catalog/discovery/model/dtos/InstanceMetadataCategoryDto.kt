package no.novari.flyt.catalog.discovery.model.dtos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class InstanceMetadataCategoryDto(
    @field:NotBlank
    val displayName: String? = null,
    @field:NotNull
    @field:Valid
    var content: InstanceMetadataContentDto? = null,
)
