package no.novari.flyt.catalog.configuration.model.metadata

data class IntegrationMetadata(
    var sourceApplicationId: Long? = null,
    var sourceApplicationIntegrationId: String? = null,
    var sourceApplicationIntegrationUri: String? = null,
    var integrationDisplayName: String? = null,
    var version: Long? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var sourceApplicationId: Long? = null
        private var sourceApplicationIntegrationId: String? = null
        private var sourceApplicationIntegrationUri: String? = null
        private var integrationDisplayName: String? = null
        private var version: Long? = null

        fun sourceApplicationId(sourceApplicationId: Long?) = apply { this.sourceApplicationId = sourceApplicationId }

        fun sourceApplicationIntegrationId(sourceApplicationIntegrationId: String?) =
            apply { this.sourceApplicationIntegrationId = sourceApplicationIntegrationId }

        fun sourceApplicationIntegrationUri(sourceApplicationIntegrationUri: String?) =
            apply { this.sourceApplicationIntegrationUri = sourceApplicationIntegrationUri }

        fun integrationDisplayName(integrationDisplayName: String?) =
            apply { this.integrationDisplayName = integrationDisplayName }

        fun version(version: Long?) = apply { this.version = version }

        fun build(): IntegrationMetadata =
            IntegrationMetadata(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationIntegrationId = sourceApplicationIntegrationId,
                sourceApplicationIntegrationUri = sourceApplicationIntegrationUri,
                integrationDisplayName = integrationDisplayName,
                version = version,
            )
    }
}
