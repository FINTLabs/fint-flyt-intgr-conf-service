package no.novari.flyt.catalog.configuration.model.metadata

data class InstanceValueMetadata(
    var displayName: String? = null,
    var key: String? = null,
    var type: Type? = null,
) {
    enum class Type {
        STRING,
        BOOLEAN,
        FILE,
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var displayName: String? = null
        private var key: String? = null
        private var type: Type? = null

        fun displayName(displayName: String?) = apply { this.displayName = displayName }

        fun key(key: String?) = apply { this.key = key }

        fun type(type: Type?) = apply { this.type = type }

        fun build(): InstanceValueMetadata = InstanceValueMetadata(displayName = displayName, key = key, type = type)
    }
}
