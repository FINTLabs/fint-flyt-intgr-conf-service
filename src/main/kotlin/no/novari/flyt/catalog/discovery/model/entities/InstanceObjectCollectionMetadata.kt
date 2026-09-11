package no.novari.flyt.catalog.discovery.model.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne

@Entity
class InstanceObjectCollectionMetadata(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonIgnore
    var id: Long = 0,
    var displayName: String? = null,
    @field:OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    var objectMetadata: InstanceMetadataContent? = null,
    @field:Column(name = "\"key\"")
    var key: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InstanceObjectCollectionMetadata) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
