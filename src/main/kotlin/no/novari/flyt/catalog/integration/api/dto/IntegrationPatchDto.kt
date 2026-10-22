package no.novari.flyt.catalog.integration.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.novari.flyt.catalog.integration.persistence.entity.Integration

@Schema(description = "Fields that can be changed on a Flyt integration.")
data class IntegrationPatchDto(
    val destination: String? = null,
    val state: Integration.State? = null,
    val activeConfigurationId: Long? = null,
)
