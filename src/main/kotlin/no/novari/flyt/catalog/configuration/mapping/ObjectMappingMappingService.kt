package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ObjectMapping
import org.springframework.stereotype.Service

@Service
class ObjectMappingMappingService(
    private val perKeyMappingService: PerKeyMappingService,
    private val valueMappingMappingService: ValueMappingMappingService,
    private val valueCollectionMappingMappingService: ValueCollectionMappingMappingService,
    private val objectCollectionMappingMappingService: ObjectCollectionMappingMappingService,
) {
    fun toEntity(objectMappingDto: ObjectMappingDto): ObjectMapping =
        ObjectMapping
            .builder()
            .valueMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMappingDto.valueMappingPerKey,
                    valueMappingMappingService::toEntity,
                ),
            ).valueCollectionMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMappingDto.valueCollectionMappingPerKey,
                    valueCollectionMappingMappingService::toEntity,
                ),
            ).objectMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMappingDto.objectMappingPerKey,
                    this::toEntity,
                ),
            ).objectCollectionMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMappingDto.objectCollectionMappingPerKey,
                    objectCollectionMappingMappingService::toEntity,
                ),
            ).build()

    fun toDto(objectMapping: ObjectMapping): ObjectMappingDto =
        ObjectMappingDto
            .builder()
            .valueMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMapping.valueMappingPerKey,
                    valueMappingMappingService::toDto,
                ),
            ).valueCollectionMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMapping.valueCollectionMappingPerKey,
                    valueCollectionMappingMappingService::toDto,
                ),
            ).objectMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMapping.objectMappingPerKey,
                    this::toDto,
                ),
            ).objectCollectionMappingPerKey(
                perKeyMappingService.mapPerKey(
                    objectMapping.objectCollectionMappingPerKey,
                    objectCollectionMappingMappingService::toDto,
                ),
            ).build()
}
