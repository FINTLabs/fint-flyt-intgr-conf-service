package no.novari.flyt.catalog.configuration

import jakarta.persistence.EntityManager
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.audit.history.EnversHistoryService
import no.novari.flyt.catalog.configuration.mapping.ConfigurationMappingService
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationSnapshot
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ConfigurationHistoryService(
    @Qualifier("configurationEntityManager") entityManager: EntityManager,
    displayResolver: ActorDisplayResolver,
    private val configurationMappingService: ConfigurationMappingService,
) : EnversHistoryService<Configuration, Long, ConfigurationSnapshot>(
        Configuration::class.java,
        entityManager,
        displayResolver,
    ) {
    public override fun mapSnapshot(entity: Configuration) = configurationMappingService.toSnapshot(entity)
}
