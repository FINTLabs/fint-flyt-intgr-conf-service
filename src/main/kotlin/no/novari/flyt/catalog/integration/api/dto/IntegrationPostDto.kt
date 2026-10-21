package no.novari.flyt.catalog.integration.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Request to create a Flyt integration.")
data class IntegrationPostDto(
    @field:NotNull
    val sourceApplicationId: Long? = null,
    @field:NotBlank
    val sourceApplicationIntegrationId: String? = null,
    @field:NotBlank
    val destination: String? = null,
)
