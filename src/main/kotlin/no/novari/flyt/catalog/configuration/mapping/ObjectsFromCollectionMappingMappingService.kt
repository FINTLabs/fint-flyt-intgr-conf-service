package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.catalog.configuration.model.configuration.dtos.FromCollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.collection.ObjectsFromCollectionMapping
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class ObjectsFromCollectionMappingMappingService(
    private val instanceCollectionReferencesMappingService: InstanceCollectionReferencesMappingService,
    @param:Lazy private val objectMappingMappingService: ObjectMappingMappingService,
) {
    fun toEntity(
        objectsFromCollectionMappingDto: FromCollectionMappingDto<ObjectMappingDto>,
    ): ObjectsFromCollectionMapping =
        ObjectsFromCollectionMapping
            .builder()
            .instanceCollectionReferencesOrdered(
                instanceCollectionReferencesMappingService.toEntity(
                    objectsFromCollectionMappingDto.instanceCollectionReferencesOrdered,
                ),
            ).objectMapping(
                objectMappingMappingService.toEntity(
                    requireNotNull(objectsFromCollectionMappingDto.elementMapping),
                ),
            ).build()

    fun toDto(objectsFromCollectionMapping: ObjectsFromCollectionMapping): FromCollectionMappingDto<ObjectMappingDto> =
        FromCollectionMappingDto
            .builder<ObjectMappingDto>()
            .instanceCollectionReferencesOrdered(
                instanceCollectionReferencesMappingService.toDto(
                    objectsFromCollectionMapping.instanceCollectionReferencesOrdered,
                ),
            ).elementMapping(
                objectMappingMappingService.toDto(
                    requireNotNull(objectsFromCollectionMapping.objectMapping),
                ),
            ).build()
}
