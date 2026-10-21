package no.novari.flyt.catalog.discovery.model.entities

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import no.novari.flyt.audit.entity.CreatedAuditedEntity

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "UniqueSourceApplicationIdAndSourceApplicationIntegrationIdAndVersion",
            columnNames = ["sourceApplicationId", "sourceApplicationIntegrationId", "version"],
        ),
    ],
)
class IntegrationMetadata(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var id: Long = 0,
    @field:Column(nullable = false)
    var sourceApplicationId: Long? = null,
    @field:Column(nullable = false)
    var sourceApplicationIntegrationId: String? = null,
    var sourceApplicationIntegrationUri: String? = null,
    @field:Column(nullable = false)
    var integrationDisplayName: String? = null,
    @field:Column(nullable = false)
    var version: Long? = null,
    @field:OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @field:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var instanceMetadata: InstanceMetadataContent? = null,
) : CreatedAuditedEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is IntegrationMetadata) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
