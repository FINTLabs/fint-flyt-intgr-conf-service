package no.novari.flyt.catalog.integration.application

import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.springframework.stereotype.Service

@Service
class IntegrationMappingService(
    private val actorDisplayResolver: ActorDisplayResolver,
) {
    fun toIntegration(integrationPostDto: IntegrationPostDto): Integration {
        return Integration(
            sourceApplicationId = requireNotNull(integrationPostDto.sourceApplicationId),
            sourceApplicationIntegrationId = requireNotNull(integrationPostDto.sourceApplicationIntegrationId),
            destination = requireNotNull(integrationPostDto.destination),
            state = Integration.State.DEACTIVATED,
        )
    }

    fun toDtos(integrations: Collection<Integration>): List<IntegrationDto> {
        val createdByDisplays = actorDisplayResolver.resolveAll(integrations.map { it.createdBy })
        val lastModifiedByDisplays = actorDisplayResolver.resolveAll(integrations.map { it.lastModifiedBy })
        return integrations.map {
            toDto(it, createdByDisplays[it.createdBy], lastModifiedByDisplays[it.lastModifiedBy])
        }
    }

    fun toDto(integration: Integration): IntegrationDto {
        return toDto(
            integration,
            actorDisplayResolver.resolve(integration.createdBy),
            actorDisplayResolver.resolve(integration.lastModifiedBy),
        )
    }

    private fun toDto(
        integration: Integration,
        createdByDisplay: String?,
        lastModifiedByDisplay: String?,
    ): IntegrationDto {
        return IntegrationDto(
            id = requireNotNull(integration.id),
            sourceApplicationId = requireNotNull(integration.sourceApplicationId),
            sourceApplicationIntegrationId = requireNotNull(integration.sourceApplicationIntegrationId),
            destination = requireNotNull(integration.destination),
            state = requireNotNull(integration.state),
            activeConfigurationId = integration.activeConfigurationId,
            createdAt = integration.createdAt,
            createdBy = createdByDisplay,
            createdByActor = integration.createdBy,
            lastModifiedAt = integration.lastModifiedAt,
            lastModifiedBy = lastModifiedByDisplay,
            lastModifiedByActor = integration.lastModifiedBy,
        )
    }
}
