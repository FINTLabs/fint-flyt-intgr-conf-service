package no.novari.flyt.catalog.configuration.model.configuration.entities.collection

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.novari.flyt.catalog.configuration.model.configuration.entities.ObjectMapping

@Entity
class ObjectCollectionMapping(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "collection_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "element_mapping_id")],
    )
    var objectMappings: MutableCollection<
        @Valid @NotNull
        ObjectMapping,
    > = mutableListOf(),
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "collection_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "from_collection_mapping_id")],
    )
    var objectsFromCollectionMappings: MutableCollection<
        @Valid @NotNull
        ObjectsFromCollectionMapping,
    > =
        mutableListOf(),
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: Long = 0
        private var objectMappings: MutableCollection<ObjectMapping> = mutableListOf()
        private var objectsFromCollectionMappings: MutableCollection<ObjectsFromCollectionMapping> = mutableListOf()

        fun id(id: Long) = apply { this.id = id }

        fun objectMappings(objectMappings: Collection<ObjectMapping>?) =
            apply { this.objectMappings = objectMappings?.toMutableList() ?: mutableListOf() }

        fun objectsFromCollectionMappings(objectsFromCollectionMappings: Collection<ObjectsFromCollectionMapping>?) =
            apply {
                this.objectsFromCollectionMappings =
                    objectsFromCollectionMappings?.toMutableList() ?: mutableListOf()
            }

        fun build(): ObjectCollectionMapping =
            ObjectCollectionMapping(
                id = id,
                objectMappings = objectMappings,
                objectsFromCollectionMappings = objectsFromCollectionMappings,
            )
    }
}
