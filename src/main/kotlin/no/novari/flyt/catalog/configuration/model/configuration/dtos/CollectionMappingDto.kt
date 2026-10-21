package no.novari.flyt.catalog.configuration.model.configuration.dtos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class CollectionMappingDto<T>(
    @field:Valid
    var elementMappings: MutableCollection<@NotNull T> = mutableListOf(),
    @field:Valid
    var fromCollectionMappings: MutableCollection<@NotNull FromCollectionMappingDto<T>> = mutableListOf(),
) {
    companion object {
        @JvmStatic
        fun <T> builder(): Builder<T> = Builder()
    }

    class Builder<T> {
        private var elementMappings: MutableCollection<T> = mutableListOf()
        private var fromCollectionMappings: MutableCollection<FromCollectionMappingDto<T>> = mutableListOf()

        fun elementMappings(elementMappings: Collection<T>?) =
            apply { this.elementMappings = elementMappings?.toMutableList() ?: mutableListOf() }

        fun fromCollectionMappings(fromCollectionMappings: Collection<FromCollectionMappingDto<T>>?) =
            apply { this.fromCollectionMappings = fromCollectionMappings?.toMutableList() ?: mutableListOf() }

        fun build(): CollectionMappingDto<T> =
            CollectionMappingDto(
                elementMappings = elementMappings,
                fromCollectionMappings = fromCollectionMappings,
            )
    }
}
