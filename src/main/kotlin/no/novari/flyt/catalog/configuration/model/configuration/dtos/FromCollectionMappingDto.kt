package no.novari.flyt.catalog.configuration.model.configuration.dtos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class FromCollectionMappingDto<T>(
    @field:NotEmpty
    var instanceCollectionReferencesOrdered: MutableList<@NotBlank String> = mutableListOf(),
    @field:Valid
    @field:NotNull
    var elementMapping: T? = null,
) {
    companion object {
        @JvmStatic
        fun <T> builder(): Builder<T> = Builder()
    }

    class Builder<T> {
        private var instanceCollectionReferencesOrdered: MutableList<String> = mutableListOf()
        private var elementMapping: T? = null

        fun instanceCollectionReferencesOrdered(instanceCollectionReferencesOrdered: List<String>?) =
            apply {
                this.instanceCollectionReferencesOrdered =
                    instanceCollectionReferencesOrdered?.toMutableList() ?: mutableListOf()
            }

        fun elementMapping(elementMapping: T?) = apply { this.elementMapping = elementMapping }

        fun build(): FromCollectionMappingDto<T> =
            FromCollectionMappingDto(
                instanceCollectionReferencesOrdered = instanceCollectionReferencesOrdered,
                elementMapping = elementMapping,
            )
    }
}
