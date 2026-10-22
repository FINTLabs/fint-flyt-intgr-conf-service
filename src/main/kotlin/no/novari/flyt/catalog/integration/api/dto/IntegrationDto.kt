package no.novari.flyt.catalog.integration.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.GroupSequence
import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.catalog.integration.validation.constraints.ActiveConfigurationIsDefinedIfStateIsActive
import no.novari.flyt.catalog.integration.validation.constraints.ReferencedConfigurationExists
import no.novari.flyt.catalog.integration.validation.constraints.ReferencedConfigurationIsComplete
import no.novari.flyt.catalog.integration.validation.constraints.ReferencedConfigurationIsForIntegration
import no.novari.flyt.catalog.integration.validation.groups.ActiveConfigurationIsCompleteGroup
import no.novari.flyt.catalog.integration.validation.groups.ActiveConfigurationIsForIntegrationGroup
import java.time.Instant

@GroupSequence(
    IntegrationDto::class,
    ActiveConfigurationIsForIntegrationGroup::class,
    ActiveConfigurationIsCompleteGroup::class,
)
@Schema(description = "A Flyt integration definition.")
data class IntegrationDto(
    val id: Long,
    val sourceApplicationId: Long,
    val sourceApplicationIntegrationId: String,
    val destination: String,
    @field:ActiveConfigurationIsDefinedIfStateIsActive
    val state: Integration.State,
    @field:ReferencedConfigurationExists
    @field:ReferencedConfigurationIsForIntegration(groups = [ActiveConfigurationIsForIntegrationGroup::class])
    @field:ReferencedConfigurationIsComplete(groups = [ActiveConfigurationIsCompleteGroup::class])
    val activeConfigurationId: Long? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val createdAt: Instant? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val createdBy: String? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val createdByActor: Actor? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val lastModifiedAt: Instant? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val lastModifiedBy: String? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val lastModifiedByActor: Actor? = null,
)
