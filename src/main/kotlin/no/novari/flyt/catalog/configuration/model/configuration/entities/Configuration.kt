package no.novari.flyt.catalog.configuration.model.configuration.entities

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.novari.flyt.audit.entity.AuditedEntity
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode

@Entity
@Audited
@Table(
    name = "configuration",
    uniqueConstraints = [
        UniqueConstraint(
            name = "unique_integration_id_version",
            columnNames = ["integration_id", "version"],
        ),
    ],
)
class Configuration(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var id: Long? = null,
    @field:NotNull
    @field:Column(name = "integration_id", nullable = false)
    var integrationId: Long? = null,
    @field:NotNull
    @field:Column(name = "integration_metadata_id", nullable = false)
    var integrationMetadataId: Long? = null,
    @field:Column(name = "version")
    var version: Int? = null,
    @field:Column(name = "completed", nullable = false)
    var completed: Boolean = false,
    @field:Column(name = "comment", columnDefinition = "text")
    var comment: String? = null,
    @field:OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JoinColumn(name = "mapping_id", referencedColumnName = "id", nullable = false)
    @field:Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @field:Valid
    @field:NotNull
    var mapping: ObjectMapping? = null,
    @field:NotAudited
    @field:Column(name = "last_modified_by_legacy", insertable = false, updatable = false)
    var lastModifiedByLegacy: String? = null,
) : AuditedEntity() {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: Long? = null
        private var integrationId: Long? = null
        private var integrationMetadataId: Long? = null
        private var version: Int? = null
        private var completed: Boolean = false
        private var comment: String? = null
        private var mapping: ObjectMapping? = null

        fun id(id: Long?) = apply { this.id = id }

        fun integrationId(integrationId: Long?) = apply { this.integrationId = integrationId }

        fun integrationMetadataId(integrationMetadataId: Long?) =
            apply {
                this.integrationMetadataId =
                    integrationMetadataId
            }

        fun version(version: Int?) = apply { this.version = version }

        fun completed(completed: Boolean) = apply { this.completed = completed }

        fun comment(comment: String?) = apply { this.comment = comment }

        fun mapping(mapping: ObjectMapping?) = apply { this.mapping = mapping }

        fun build(): Configuration =
            Configuration(
                id = id,
                integrationId = integrationId,
                integrationMetadataId = integrationMetadataId,
                version = version,
                completed = completed,
                comment = comment,
                mapping = mapping,
            )
    }
}
