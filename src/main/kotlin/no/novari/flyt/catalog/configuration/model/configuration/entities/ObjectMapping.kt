package no.novari.flyt.catalog.configuration.model.configuration.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.OneToMany
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.novari.flyt.catalog.configuration.model.configuration.entities.collection.ObjectCollectionMapping
import no.novari.flyt.catalog.configuration.model.configuration.entities.collection.ValueCollectionMapping

@Entity
class ObjectMapping(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "object_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "value_mapping_id")],
    )
    @field:MapKeyColumn(name = "key")
    var valueMappingPerKey: MutableMap<
        String,
        @Valid @NotNull
        ValueMapping,
    > = mutableMapOf(),
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "object_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "value_collection_mapping_id")],
    )
    @field:MapKeyColumn(name = "key")
    var valueCollectionMappingPerKey: MutableMap<
        String,
        @Valid @NotNull
        ValueCollectionMapping,
    > = mutableMapOf(),
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "parent_id")],
        inverseJoinColumns = [JoinColumn(name = "child_id")],
    )
    @field:MapKeyColumn(name = "key")
    var objectMappingPerKey: MutableMap<
        String,
        @Valid @NotNull
        ObjectMapping,
    > = mutableMapOf(),
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "object_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "object_collection_mapping_id")],
    )
    @field:MapKeyColumn(name = "key")
    var objectCollectionMappingPerKey: MutableMap<
        String,
        @Valid @NotNull
        ObjectCollectionMapping,
    > = mutableMapOf(),
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: Long = 0
        private var valueMappingPerKey: MutableMap<String, ValueMapping> = mutableMapOf()
        private var valueCollectionMappingPerKey: MutableMap<String, ValueCollectionMapping> = mutableMapOf()
        private var objectMappingPerKey: MutableMap<String, ObjectMapping> = mutableMapOf()
        private var objectCollectionMappingPerKey: MutableMap<String, ObjectCollectionMapping> = mutableMapOf()

        fun id(id: Long) = apply { this.id = id }

        fun valueMappingPerKey(valueMappingPerKey: Map<String, ValueMapping>?) =
            apply { this.valueMappingPerKey = valueMappingPerKey?.toMutableMap() ?: mutableMapOf() }

        fun valueCollectionMappingPerKey(valueCollectionMappingPerKey: Map<String, ValueCollectionMapping>?) =
            apply { this.valueCollectionMappingPerKey = valueCollectionMappingPerKey?.toMutableMap() ?: mutableMapOf() }

        fun objectMappingPerKey(objectMappingPerKey: Map<String, ObjectMapping>?) =
            apply { this.objectMappingPerKey = objectMappingPerKey?.toMutableMap() ?: mutableMapOf() }

        fun objectCollectionMappingPerKey(objectCollectionMappingPerKey: Map<String, ObjectCollectionMapping>?) =
            apply {
                this.objectCollectionMappingPerKey = objectCollectionMappingPerKey?.toMutableMap() ?: mutableMapOf()
            }

        fun build(): ObjectMapping =
            ObjectMapping(
                id = id,
                valueMappingPerKey = valueMappingPerKey,
                valueCollectionMappingPerKey = valueCollectionMappingPerKey,
                objectMappingPerKey = objectMappingPerKey,
                objectCollectionMappingPerKey = objectCollectionMappingPerKey,
            )
    }
}
