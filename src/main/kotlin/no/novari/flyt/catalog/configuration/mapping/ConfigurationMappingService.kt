package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationSnapshot
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import no.novari.flyt.catalog.configuration.model.configuration.entities.ObjectMapping
import org.springframework.stereotype.Service

@Service
class ConfigurationMappingService(
    private val objectMappingMappingService: ObjectMappingMappingService,
    private val actorDisplayResolver: ActorDisplayResolver,
) {
    fun toEntity(configurationDto: ConfigurationDto): Configuration =
        Configuration
            .builder()
            .integrationId(configurationDto.integrationId)
            .integrationMetadataId(configurationDto.integrationMetadataId)
            .comment(configurationDto.comment)
            .completed(configurationDto.completed)
            .mapping(toEntity(requireNotNull(configurationDto.mapping)))
            .build()

    fun toDto(
        configuration: Configuration,
        excludeMapping: Boolean,
    ): ConfigurationDto = toDto(configuration, excludeMapping, actorDisplayResolver.resolveAll(actorsOf(configuration)))

    fun toDtos(
        configurations: List<Configuration>,
        excludeMapping: Boolean,
    ): List<ConfigurationDto> {
        val displays = actorDisplayResolver.resolveAll(configurations.flatMap(::actorsOf))
        return configurations.map { toDto(it, excludeMapping, displays) }
    }

    fun toSnapshot(configuration: Configuration): ConfigurationSnapshot =
        ConfigurationSnapshot(
            id = configuration.id,
            integrationId = configuration.integrationId,
            integrationMetadataId = configuration.integrationMetadataId,
            version = configuration.version,
            completed = configuration.completed,
            comment = configuration.comment,
        )

    fun toEntity(objectMappingDto: ObjectMappingDto): ObjectMapping =
        objectMappingMappingService.toEntity(objectMappingDto)

    fun toDto(objectMapping: ObjectMapping): ObjectMappingDto = objectMappingMappingService.toDto(objectMapping)

    private fun toDto(
        configuration: Configuration,
        excludeMapping: Boolean,
        displays: Map<Actor, String?>,
    ): ConfigurationDto {
        return ConfigurationDto
            .builder()
            .id(configuration.id)
            .integrationId(configuration.integrationId)
            .integrationMetadataId(configuration.integrationMetadataId)
            .version(configuration.version)
            .completed(configuration.completed)
            .comment(configuration.comment)
            .mapping(configuration.mapping?.takeUnless { excludeMapping }?.let(::toDto))
            .createdAt(configuration.createdAt)
            .createdBy(configuration.createdBy?.let { displays[it] })
            .createdByActor(configuration.createdBy)
            .lastModifiedAt(configuration.lastModifiedAt)
            .lastModifiedBy(lastModifiedByDisplay(configuration, displays))
            .lastModifiedByActor(configuration.lastModifiedBy)
            .build()
    }

    /**
     * Rader fra før auditing ble innført fikk aktør `UNKNOWN`, men det opprinnelige navnet er bevart
     * i `last_modified_by_legacy`. Legacy-navnet brukes derfor kun så lenge aktøren er ukjent — så
     * snart raden endres av en identifisert aktør, er det den som skal vises.
     */
    private fun lastModifiedByDisplay(
        configuration: Configuration,
        displays: Map<Actor, String?>,
    ): String? {
        val actor = configuration.lastModifiedBy
        if (actor != null && actor != Actor.Unknown) {
            return displays[actor]
        }
        return configuration.lastModifiedByLegacy ?: actor?.let { displays[it] }
    }

    private fun actorsOf(configuration: Configuration): List<Actor?> =
        listOf(configuration.createdBy, configuration.lastModifiedBy)
}
