package no.novari.flyt.catalog.configuration.model.configuration.dtos

import jakarta.validation.constraints.NotNull
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.constraints.InstanceValueKeysAreDefinedInMetadata
import no.novari.flyt.catalog.configuration.validation.constraints.InstanceValueTypesAreCompatible
import no.novari.flyt.catalog.configuration.validation.constraints.ValueParsableAsType
import no.novari.flyt.catalog.configuration.validation.groups.InstanceValueKeys
import no.novari.flyt.catalog.configuration.validation.groups.InstanceValueTypes
import no.novari.flyt.catalog.configuration.validation.groups.ValueParsability

@ValueParsableAsType(groups = [ValueParsability::class])
@InstanceValueKeysAreDefinedInMetadata(groups = [InstanceValueKeys::class])
@InstanceValueTypesAreCompatible(groups = [InstanceValueTypes::class])
data class ValueMappingDto(
    @field:NotNull
    var type: ValueMapping.Type? = null,
    @field:NotNull
    var mappingString: String? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var type: ValueMapping.Type? = null
        private var mappingString: String? = null

        fun type(type: ValueMapping.Type?) = apply { this.type = type }

        fun mappingString(mappingString: String?) = apply { this.mappingString = mappingString }

        fun build(): ValueMappingDto = ValueMappingDto(type = type, mappingString = mappingString)
    }
}
