package no.novari.flyt.catalog.configuration.validation

import jakarta.validation.Payload
import no.novari.flyt.catalog.configuration.model.integration.Integration
import no.novari.flyt.catalog.configuration.model.metadata.InstanceValueMetadata
import no.novari.flyt.catalog.configuration.model.metadata.IntegrationMetadata

data class ConfigurationValidationContext(
    var integration: Integration? = null,
    var metadata: IntegrationMetadata? = null,
    var instanceValueTypePerKey: Map<String, InstanceValueMetadata.Type> = emptyMap(),
) : Payload {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var integration: Integration? = null
        private var metadata: IntegrationMetadata? = null
        private var instanceValueTypePerKey: Map<String, InstanceValueMetadata.Type> = emptyMap()

        fun integration(integration: Integration?) = apply { this.integration = integration }

        fun metadata(metadata: IntegrationMetadata?) = apply { this.metadata = metadata }

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
