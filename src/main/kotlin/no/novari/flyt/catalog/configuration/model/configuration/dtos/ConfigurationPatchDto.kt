package no.novari.flyt.catalog.configuration.model.configuration.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Fields that can be changed on a configuration draft.")
data class ConfigurationPatchDto(
    val integrationMetadataId: Long? = null,
    val completed: Boolean? = null,
    val comment: String? = null,
    val mapping: ObjectMappingDto? = null,
)
