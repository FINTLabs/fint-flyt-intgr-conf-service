package no.novari.flyt.catalog.discovery.model.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne

@Entity
class InstanceMetadataCategory(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonIgnore
    var id: Long = 0,
    var displayName: String? = null,
    @field:OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    var content: InstanceMetadataContent? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InstanceMetadataCategory) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
