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
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping

@Entity
class ValueCollectionMapping(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "collection_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "element_mapping_id")],
    )
    var valueMappings: MutableCollection<
        @Valid @NotNull
        ValueMapping,
    > = mutableListOf(),
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        joinColumns = [JoinColumn(name = "collection_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "from_collection_mapping_id")],
    )
    var valuesFromCollectionMappings: MutableCollection<
        @Valid @NotNull
        ValuesFromCollectionMapping,
    > = mutableListOf(),
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: Long = 0
        private var valueMappings: MutableCollection<ValueMapping> = mutableListOf()
        private var valuesFromCollectionMappings: MutableCollection<ValuesFromCollectionMapping> = mutableListOf()

        fun id(id: Long) = apply { this.id = id }

        fun valueMappings(valueMappings: Collection<ValueMapping>?) =
            apply { this.valueMappings = valueMappings?.toMutableList() ?: mutableListOf() }

        fun valuesFromCollectionMappings(valuesFromCollectionMappings: Collection<ValuesFromCollectionMapping>?) =
            apply {
                this.valuesFromCollectionMappings = valuesFromCollectionMappings?.toMutableList() ?: mutableListOf()
            }

        fun build(): ValueCollectionMapping =
            ValueCollectionMapping(
                id = id,
                valueMappings = valueMappings,
                valuesFromCollectionMappings = valuesFromCollectionMappings,
            )
    }
}
