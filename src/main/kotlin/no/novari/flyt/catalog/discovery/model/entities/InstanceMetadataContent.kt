package no.novari.flyt.catalog.discovery.model.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
class InstanceMetadataContent(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonIgnore
    var id: Long = 0,
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    var instanceValueMetadata: MutableList<InstanceValueMetadata> = mutableListOf(),
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    var instanceObjectCollectionMetadata: MutableList<InstanceObjectCollectionMetadata> = mutableListOf(),
    @field:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    var categories: MutableList<InstanceMetadataCategory> = mutableListOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InstanceMetadataContent) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
