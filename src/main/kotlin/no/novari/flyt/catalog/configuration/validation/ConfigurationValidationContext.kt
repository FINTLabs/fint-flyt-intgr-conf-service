package no.novari.flyt.catalog.configuration.validation

import jakarta.validation.Payload
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto

data class ConfigurationValidationContext(
    var integration: IntegrationDto? = null,
    var metadata: IntegrationMetadataDto? = null,
    var instanceValueTypePerKey: Map<String, InstanceValueMetadata.Type> = emptyMap(),
) : Payload {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var integration: IntegrationDto? = null
        private var metadata: IntegrationMetadataDto? = null
        private var instanceValueTypePerKey: Map<String, InstanceValueMetadata.Type> = emptyMap()

        fun integration(integration: IntegrationDto?) = apply { this.integration = integration }

        fun metadata(metadata: IntegrationMetadataDto?) = apply { this.metadata = metadata }

        fun instanceValueTypePerKey(instanceValueTypePerKey: Map<String, InstanceValueMetadata.Type>?) =
            apply { this.instanceValueTypePerKey = instanceValueTypePerKey ?: emptyMap() }

        fun build(): ConfigurationValidationContext =
            ConfigurationValidationContext(
                integration = integration,
                metadata = metadata,
                instanceValueTypePerKey = instanceValueTypePerKey,
            )
    }
}
