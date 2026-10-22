package no.novari.flyt.catalog.configuration.model.configuration.entities.collection

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping

@Entity
class ValuesFromCollectionMapping(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonIgnore
    var id: Long = 0,
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinTable(
        name = "values_from_collection_mapping_references_ordered",
        joinColumns = [JoinColumn(name = "values_from_collection_mapping_id")],
        inverseJoinColumns = [JoinColumn(name = "instance_collection_reference_id")],
    )
    @field:NotEmpty
    var instanceCollectionReferencesOrdered: MutableCollection<
        @Valid @NotNull
        InstanceCollectionReference,
    > =
        mutableListOf(),
    @field:OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:Valid
    @field:NotNull
    var valueMapping: ValueMapping? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: Long = 0
        private var instanceCollectionReferencesOrdered: MutableCollection<InstanceCollectionReference> =
            mutableListOf()
        private var valueMapping: ValueMapping? = null

        fun id(id: Long) = apply { this.id = id }

        fun instanceCollectionReferencesOrdered(
            instanceCollectionReferencesOrdered: Collection<InstanceCollectionReference>?,
        ) = apply {
            this.instanceCollectionReferencesOrdered =
                instanceCollectionReferencesOrdered?.toMutableList() ?: mutableListOf()
        }

        fun valueMapping(valueMapping: ValueMapping?) = apply { this.valueMapping = valueMapping }

        fun build(): ValuesFromCollectionMapping =
            ValuesFromCollectionMapping(
                id = id,
                instanceCollectionReferencesOrdered = instanceCollectionReferencesOrdered,
                valueMapping = valueMapping,
            )
    }
}
