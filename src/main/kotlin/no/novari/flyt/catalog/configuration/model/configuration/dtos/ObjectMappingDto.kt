package no.novari.flyt.catalog.configuration.model.configuration.dtos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.novari.flyt.catalog.configuration.validation.constraints.AtLeastOneChild
import no.novari.flyt.catalog.configuration.validation.constraints.UniqueChildrenKeys

@AtLeastOneChild
@UniqueChildrenKeys
data class ObjectMappingDto(
    @field:Valid
    var valueMappingPerKey: MutableMap<String, @NotNull ValueMappingDto> = mutableMapOf(),
    @field:Valid
    var valueCollectionMappingPerKey: MutableMap<String, @NotNull CollectionMappingDto<ValueMappingDto>> =
        mutableMapOf(),
    @field:Valid
    var objectMappingPerKey: MutableMap<String, @NotNull ObjectMappingDto> = mutableMapOf(),
    @field:Valid
    var objectCollectionMappingPerKey: MutableMap<String, @NotNull CollectionMappingDto<ObjectMappingDto>> =
        mutableMapOf(),
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var valueMappingPerKey: MutableMap<String, ValueMappingDto> = mutableMapOf()
        private var valueCollectionMappingPerKey: MutableMap<String, CollectionMappingDto<ValueMappingDto>> =
            mutableMapOf()
        private var objectMappingPerKey: MutableMap<String, ObjectMappingDto> = mutableMapOf()
        private var objectCollectionMappingPerKey: MutableMap<String, CollectionMappingDto<ObjectMappingDto>> =
            mutableMapOf()

        fun valueMappingPerKey(valueMappingPerKey: Map<String, ValueMappingDto>?) =
            apply { this.valueMappingPerKey = valueMappingPerKey?.toMutableMap() ?: mutableMapOf() }

        fun valueCollectionMappingPerKey(
            valueCollectionMappingPerKey: Map<String, CollectionMappingDto<ValueMappingDto>>?,
        ) = apply {
            this.valueCollectionMappingPerKey = valueCollectionMappingPerKey?.toMutableMap() ?: mutableMapOf()
        }

        fun objectMappingPerKey(objectMappingPerKey: Map<String, ObjectMappingDto>?) =
            apply { this.objectMappingPerKey = objectMappingPerKey?.toMutableMap() ?: mutableMapOf() }

        fun objectCollectionMappingPerKey(
            objectCollectionMappingPerKey: Map<String, CollectionMappingDto<ObjectMappingDto>>?,
        ) = apply {
            this.objectCollectionMappingPerKey = objectCollectionMappingPerKey?.toMutableMap() ?: mutableMapOf()
        }

        fun build(): ObjectMappingDto =
            ObjectMappingDto(
                valueMappingPerKey = valueMappingPerKey,
                valueCollectionMappingPerKey = valueCollectionMappingPerKey,
                objectMappingPerKey = objectMappingPerKey,
                objectCollectionMappingPerKey = objectCollectionMappingPerKey,
            )
    }
}
