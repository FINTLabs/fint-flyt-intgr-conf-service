package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.catalog.configuration.model.configuration.dtos.CollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.collection.ValueCollectionMapping
import org.springframework.stereotype.Service

@Service
class ValueCollectionMappingMappingService(
    private val valueMappingMappingService: ValueMappingMappingService,
    private val valuesFromCollectionMappingMappingService: ValuesFromCollectionMappingMappingService,
) {
    fun toEntity(valueCollectionMappingDto: CollectionMappingDto<ValueMappingDto>): ValueCollectionMapping =
        ValueCollectionMapping
            .builder()
            .valueMappings(valueCollectionMappingDto.elementMappings.map(valueMappingMappingService::toEntity))
            .valuesFromCollectionMappings(
                valueCollectionMappingDto.fromCollectionMappings.map(
                    valuesFromCollectionMappingMappingService::toEntity,
                ),
            ).build()

    fun toDto(valueCollectionMapping: ValueCollectionMapping): CollectionMappingDto<ValueMappingDto> =
        CollectionMappingDto
            .builder<ValueMappingDto>()
            .elementMappings(valueCollectionMapping.valueMappings.map(valueMappingMappingService::toDto))
            .fromCollectionMappings(
                valueCollectionMapping.valuesFromCollectionMappings.map(
                    valuesFromCollectionMappingMappingService::toDto,
                ),
            ).build()
}
