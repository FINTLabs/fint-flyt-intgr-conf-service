package no.novari.flyt.catalog.integration.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.novari.flyt.audit.actor.Actor
import java.time.Instant

data class ConfigurationDto(
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var id: Long? = null,
    var integrationId: Long? = null,
    var integrationMetadataId: Long? = null,
    var completed: Boolean = false,
    var comment: String? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var version: Int? = null,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
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
)
