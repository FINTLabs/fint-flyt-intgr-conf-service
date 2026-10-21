package no.novari.flyt.catalog.configuration.model.integration

data class Integration(
    var sourceApplicationId: Long? = null,
    var sourceApplicationIntegrationId: String? = null,
    var destination: String? = null,
    var state: State? = null,
    var activeConfigurationId: Long? = null,
) {
    enum class State {
        ACTIVE,
        DEACTIVATED,
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var sourceApplicationId: Long? = null
        private var sourceApplicationIntegrationId: String? = null
        private var destination: String? = null
        private var state: State? = null
        private var activeConfigurationId: Long? = null

        fun sourceApplicationId(sourceApplicationId: Long?) = apply { this.sourceApplicationId = sourceApplicationId }

        fun sourceApplicationIntegrationId(sourceApplicationIntegrationId: String?) =
            apply { this.sourceApplicationIntegrationId = sourceApplicationIntegrationId }

        fun destination(destination: String?) = apply { this.destination = destination }

        fun state(state: State?) = apply { this.state = state }

        fun activeConfigurationId(activeConfigurationId: Long?) =
            apply { this.activeConfigurationId = activeConfigurationId }

        fun build(): Integration =
            Integration(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationIntegrationId = sourceApplicationIntegrationId,
                destination = destination,
                state = state,
                activeConfigurationId = activeConfigurationId,
            )
    }
}
