package no.novari.flyt.catalog.configuration.validation.valueparsability

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping

interface ValueParsabilityValidator {
    fun isValid(valueMappingDto: ValueMappingDto): Boolean =
        valueMappingDto.type != getTypeToValidate() ||
            valueMappingDto.mappingString?.let(::isValid) != false

    fun getTypeToValidate(): ValueMapping.Type

    fun isValid(value: String): Boolean
}
