package no.novari.flyt.catalog.integration.validation

import jakarta.validation.Payload
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto

data class IntegrationValidationContext(
    val integrationId: Long? = null,
    val configuration: ConfigurationDto? = null,
) : Payload
