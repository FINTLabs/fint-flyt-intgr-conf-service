package no.novari.flyt.catalog.integration.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A page of Flyt integrations.")
data class IntegrationPageResponse(
    val content: List<IntegrationDto>,
    val totalElements: Long,
    val totalPages: Int,
)
