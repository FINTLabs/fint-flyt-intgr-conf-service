package no.novari.flyt.catalog.integration.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank
import no.novari.flyt.audit.entity.AuditedEntity

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "UniqueSourceApplicationIdAndSourceApplicationIntegrationId",
            columnNames = ["source_application_id", "source_application_integration_id"],
        ),
    ],
)
class Integration(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @field:Column(name = "source_application_id", nullable = false)
    var sourceApplicationId: Long? = null,
    @field:NotBlank
    @field:Column(name = "source_application_integration_id")
    var sourceApplicationIntegrationId: String? = null,
    @field:NotBlank
    @field:Column(name = "destination")
    var destination: String? = null,
    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "state", nullable = false)
    var state: State? = State.DEACTIVATED,
    @field:Column(name = "active_configuration_id")
    var activeConfigurationId: Long? = null,
) : AuditedEntity() {
    enum class State {
        ACTIVE,
        DEACTIVATED,
    }
}
