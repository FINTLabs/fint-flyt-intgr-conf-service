package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.catalog.configuration.model.configuration.dtos.CollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.collection.ObjectCollectionMapping
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class ObjectCollectionMappingMappingService(
    @param:Lazy private val objectMappingMappingService: ObjectMappingMappingService,
    private val objectsFromCollectionMappingMappingService: ObjectsFromCollectionMappingMappingService,
) {
    fun toEntity(objectCollectionMappingDto: CollectionMappingDto<ObjectMappingDto>): ObjectCollectionMapping =
        ObjectCollectionMapping
            .builder()
            .objectMappings(objectCollectionMappingDto.elementMappings.map(objectMappingMappingService::toEntity))
            .objectsFromCollectionMappings(
                objectCollectionMappingDto.fromCollectionMappings.map(
                    objectsFromCollectionMappingMappingService::toEntity,
                ),
            ).build()

    fun toDto(objectCollectionMapping: ObjectCollectionMapping): CollectionMappingDto<ObjectMappingDto> =
        CollectionMappingDto
            .builder<ObjectMappingDto>()
            .elementMappings(objectCollectionMapping.objectMappings.map(objectMappingMappingService::toDto))
            .fromCollectionMappings(
                objectCollectionMapping.objectsFromCollectionMappings.map(
                    objectsFromCollectionMappingMappingService::toDto,
                ),
            ).build()
}
