package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.springframework.stereotype.Service

@Service
class ValueMappingMappingService {
    fun toEntity(valueMappingDto: ValueMappingDto): ValueMapping =
        ValueMapping
            .builder()
            .type(valueMappingDto.type)
            .mappingString(valueMappingDto.mappingString)
            .build()

    fun toDto(valueMapping: ValueMapping): ValueMappingDto =
        ValueMappingDto
            .builder()
            .type(valueMapping.type)
            .mappingString(valueMapping.mappingString)
            .build()
}
