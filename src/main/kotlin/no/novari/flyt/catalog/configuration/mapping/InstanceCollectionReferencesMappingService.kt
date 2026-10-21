package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.catalog.configuration.model.configuration.entities.collection.InstanceCollectionReference
import org.springframework.stereotype.Service

@Service
class InstanceCollectionReferencesMappingService {
    fun toEntity(instanceCollectionReferencesOrderedFromDto: List<String>): List<InstanceCollectionReference> =
        instanceCollectionReferencesOrderedFromDto.mapIndexed { index, reference ->
            InstanceCollectionReference
                .builder()
                .index(index)
                .reference(reference)
                .build()
        }

    fun toDto(instanceCollectionReferencesOrdered: Collection<InstanceCollectionReference>): List<String> =
        instanceCollectionReferencesOrdered
            .sortedBy { it.index }
            .mapNotNull { it.reference }
}
