package no.novari.flyt.catalog.discovery.model.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class InstanceValueMetadata(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonIgnore
    var id: Long = 0,
    var displayName: String? = null,
    @field:Enumerated(EnumType.STRING)
    var type: Type? = null,
    @field:Column(name = "\"key\"")
    var key: String? = null,
) {
    enum class Type {
        STRING,
        BOOLEAN,
        FILE,
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InstanceValueMetadata) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
