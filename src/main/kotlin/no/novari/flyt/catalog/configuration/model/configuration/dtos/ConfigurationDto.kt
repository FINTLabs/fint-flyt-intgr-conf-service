package no.novari.flyt.catalog.configuration.model.configuration.dtos

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.configuration.validation.constraints.IntegrationAndMetadataMatches
import java.time.Instant

@IntegrationAndMetadataMatches
@Schema(description = "A versioned Flyt integration configuration.")
data class ConfigurationDto(
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var id: Long? = null,
    @field:NotNull
    var integrationId: Long? = null,
    @field:NotNull
    var integrationMetadataId: Long? = null,
    var completed: Boolean = false,
    var comment: String? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var version: Int? = null,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    @field:Valid
    @field:NotNull
    var mapping: ObjectMappingDto? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var createdAt: Instant? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var createdBy: String? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var createdByActor: Actor? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var lastModifiedAt: Instant? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var lastModifiedBy: String? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var lastModifiedByActor: Actor? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    fun toBuilder(): Builder =
        Builder()
            .id(id)
            .integrationId(integrationId)
            .integrationMetadataId(integrationMetadataId)
            .completed(completed)
            .comment(comment)
            .version(version)
            .mapping(mapping)
            .createdAt(createdAt)
            .createdBy(createdBy)
            .createdByActor(createdByActor)
            .lastModifiedAt(lastModifiedAt)
            .lastModifiedBy(lastModifiedBy)
            .lastModifiedByActor(lastModifiedByActor)

    class Builder {
        private var id: Long? = null
        private var integrationId: Long? = null
        private var integrationMetadataId: Long? = null
        private var completed: Boolean = false
        private var comment: String? = null
        private var version: Int? = null
        private var mapping: ObjectMappingDto? = null
        private var createdAt: Instant? = null
        private var createdBy: String? = null
        private var createdByActor: Actor? = null
        private var lastModifiedAt: Instant? = null
        private var lastModifiedBy: String? = null
        private var lastModifiedByActor: Actor? = null

        fun id(id: Long?) = apply { this.id = id }

        fun integrationId(integrationId: Long?) = apply { this.integrationId = integrationId }

        fun integrationMetadataId(integrationMetadataId: Long?) =
            apply { this.integrationMetadataId = integrationMetadataId }

        fun completed(completed: Boolean) = apply { this.completed = completed }

        fun comment(comment: String?) = apply { this.comment = comment }

        fun version(version: Int?) = apply { this.version = version }

        fun mapping(mapping: ObjectMappingDto?) = apply { this.mapping = mapping }

        fun createdAt(createdAt: Instant?) = apply { this.createdAt = createdAt }

        fun createdBy(createdBy: String?) = apply { this.createdBy = createdBy }

        fun createdByActor(createdByActor: Actor?) = apply { this.createdByActor = createdByActor }

        fun lastModifiedAt(lastModifiedAt: Instant?) = apply { this.lastModifiedAt = lastModifiedAt }

        fun lastModifiedBy(lastModifiedBy: String?) = apply { this.lastModifiedBy = lastModifiedBy }

        fun lastModifiedByActor(lastModifiedByActor: Actor?) = apply { this.lastModifiedByActor = lastModifiedByActor }

        fun build(): ConfigurationDto =
            ConfigurationDto(
                id = id,
                integrationId = integrationId,
                integrationMetadataId = integrationMetadataId,
                completed = completed,
                comment = comment,
                version = version,
                mapping = mapping,
                createdAt = createdAt,
                createdBy = createdBy,
                createdByActor = createdByActor,
                lastModifiedAt = lastModifiedAt,
                lastModifiedBy = lastModifiedBy,
                lastModifiedByActor = lastModifiedByActor,
            )
    }
}
