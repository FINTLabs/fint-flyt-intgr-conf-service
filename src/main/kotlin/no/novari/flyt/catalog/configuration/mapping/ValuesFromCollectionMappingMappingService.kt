package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.catalog.configuration.model.configuration.dtos.FromCollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.collection.ValuesFromCollectionMapping
import org.springframework.stereotype.Service

@Service
class ValuesFromCollectionMappingMappingService(
    private val instanceCollectionReferencesMappingService: InstanceCollectionReferencesMappingService,
    private val valueMappingMappingService: ValueMappingMappingService,
) {
    fun toEntity(
        valuesFromCollectionMappingDto: FromCollectionMappingDto<ValueMappingDto>,
    ): ValuesFromCollectionMapping =
        ValuesFromCollectionMapping
            .builder()
            .instanceCollectionReferencesOrdered(
                instanceCollectionReferencesMappingService.toEntity(
                    valuesFromCollectionMappingDto.instanceCollectionReferencesOrdered,
                ),
            ).valueMapping(
                valueMappingMappingService.toEntity(
                    requireNotNull(valuesFromCollectionMappingDto.elementMapping),
                ),
            ).build()

    fun toDto(valuesFromCollectionMapping: ValuesFromCollectionMapping): FromCollectionMappingDto<ValueMappingDto> =
        FromCollectionMappingDto
            .builder<ValueMappingDto>()
            .instanceCollectionReferencesOrdered(
                instanceCollectionReferencesMappingService.toDto(
                    valuesFromCollectionMapping.instanceCollectionReferencesOrdered,
                ),
            ).elementMapping(
                valueMappingMappingService.toDto(
                    requireNotNull(valuesFromCollectionMapping.valueMapping),
                ),
            ).build()
}
