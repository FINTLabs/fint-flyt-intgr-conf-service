package no.novari.flyt.catalog.configuration.model.configuration.dtos

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A page of Flyt configurations.")
data class ConfigurationPageResponse(
    val content: List<ConfigurationDto>,
    val totalElements: Long,
    val totalPages: Int,
)
