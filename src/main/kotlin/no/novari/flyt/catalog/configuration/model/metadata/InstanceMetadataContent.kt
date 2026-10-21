package no.novari.flyt.catalog.configuration.model.metadata

data class InstanceMetadataContent(
    var instanceValueMetadata: MutableCollection<InstanceValueMetadata> = mutableListOf(),
    var instanceObjectCollectionMetadata: MutableCollection<InstanceObjectCollectionMetadata> = mutableListOf(),
    var categories: MutableCollection<InstanceMetadataCategory> = mutableListOf(),
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var instanceValueMetadata: MutableCollection<InstanceValueMetadata> = mutableListOf()
        private var instanceObjectCollectionMetadata: MutableCollection<InstanceObjectCollectionMetadata> =
            mutableListOf()
        private var categories: MutableCollection<InstanceMetadataCategory> = mutableListOf()

        fun instanceValueMetadata(instanceValueMetadata: Collection<InstanceValueMetadata>?) =
            apply { this.instanceValueMetadata = instanceValueMetadata?.toMutableList() ?: mutableListOf() }

        fun instanceObjectCollectionMetadata(
            instanceObjectCollectionMetadata: Collection<InstanceObjectCollectionMetadata>?,
        ) = apply {
            this.instanceObjectCollectionMetadata = instanceObjectCollectionMetadata?.toMutableList() ?: mutableListOf()
        }

        fun categories(categories: Collection<InstanceMetadataCategory>?) =
            apply { this.categories = categories?.toMutableList() ?: mutableListOf() }

        fun build(): InstanceMetadataContent =
            InstanceMetadataContent(
                instanceValueMetadata = instanceValueMetadata,
                instanceObjectCollectionMetadata = instanceObjectCollectionMetadata,
                categories = categories,
            )
    }
}
