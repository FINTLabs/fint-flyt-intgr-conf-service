package no.novari.flyt.catalog.integration.application

import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPatchDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.persistence.IntegrationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class IntegrationService(
    private val integrationRepository: IntegrationRepository,
    private val integrationMappingService: IntegrationMappingService,
) {
    fun findDistinctSourceApplicationIds(): Set<Long> = integrationRepository.findDistinctSourceApplicationIds()

    fun findAll(): Collection<IntegrationDto> {
        return integrationMappingService.toDtos(integrationRepository.findAll())
    }

    fun findAll(pageable: Pageable): Page<IntegrationDto> {
        return integrationRepository.findAll(pageable).map(integrationMappingService::toDto)
    }

    fun findAllBySourceApplicationIds(sourceApplicationIds: Set<Long>): Collection<IntegrationDto> {
        return integrationMappingService.toDtos(
            integrationRepository.findIntegrationsBySourceApplicationIdIn(sourceApplicationIds),
        )
    }

    fun findAllBySourceApplicationIds(
        sourceApplicationIds: Set<Long>,
        pageable: Pageable,
    ): Page<IntegrationDto> {
        return integrationRepository
            .findIntegrationsBySourceApplicationIdIn(sourceApplicationIds, pageable)
            .map(integrationMappingService::toDto)
    }

    fun existsById(integrationId: Long): Boolean {
        return integrationRepository.existsById(integrationId)
    }

    fun findById(integrationId: Long): IntegrationDto? {
        return integrationRepository
            .findById(integrationId)
            .orElse(null)
            ?.let(integrationMappingService::toDto)
    }

    fun findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
    ): IntegrationDto? {
        return integrationRepository
            .findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                sourceApplicationId,
                sourceApplicationIntegrationId,
            )?.let(integrationMappingService::toDto)
    }

    fun existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
    ): Boolean {
        return integrationRepository.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
            sourceApplicationId,
            sourceApplicationIntegrationId,
        )
    }

    fun findActiveConfigurationIdByIntegrationId(integrationId: Long): Long? {
        return integrationRepository.findById(integrationId).orElse(null)?.activeConfigurationId
    }

    fun save(integrationPostDto: IntegrationPostDto): IntegrationDto {
        return integrationMappingService.toDto(
            integrationRepository.save(integrationMappingService.toIntegration(integrationPostDto)),
        )
    }

    fun updateById(
        integrationId: Long,
        integrationPatchDto: IntegrationPatchDto,
    ): IntegrationDto {
        val integration = integrationRepository.getReferenceById(integrationId)

        integrationPatchDto.destination?.let { integration.destination = it }
        integrationPatchDto.state?.let { integration.state = it }
        integrationPatchDto.activeConfigurationId?.let { integration.activeConfigurationId = it }

        return integrationMappingService.toDto(integrationRepository.save(integration))
    }
}
