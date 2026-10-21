package no.novari.flyt.catalog.configuration.model.metadata

data class InstanceObjectCollectionMetadata(
    var displayName: String? = null,
    var key: String? = null,
    @Suppress("unused")
    var objectMetadata: InstanceMetadataContent? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var displayName: String? = null
        private var key: String? = null
        private var objectMetadata: InstanceMetadataContent? = null

        fun displayName(displayName: String?) = apply { this.displayName = displayName }

        fun key(key: String?) = apply { this.key = key }

        fun objectMetadata(objectMetadata: InstanceMetadataContent?) = apply { this.objectMetadata = objectMetadata }

        fun build(): InstanceObjectCollectionMetadata =
            InstanceObjectCollectionMetadata(displayName = displayName, key = key, objectMetadata = objectMetadata)
    }
}
