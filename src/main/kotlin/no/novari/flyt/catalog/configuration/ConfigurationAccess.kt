package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.integration.persistence.IntegrationRepository
import no.novari.flyt.catalog.security.AuthorizationService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/**
 * A configuration belongs to the source application of its integration (FFS-2431). The check is against the stored
 * integration, so a request cannot choose which source application is checked.
 */
@Service
class ConfigurationAccess(
    private val integrationRepository: IntegrationRepository,
    private val authorizationService: AuthorizationService,
) {
    /** 403 without access. An unknown integration is left to the caller: validation gives 422 for a new one. */
    fun checkAccessToIntegration(integrationId: Long) {
        val sourceApplicationId = integrationRepository.findByIdOrNull(integrationId)?.sourceApplicationId ?: return
        authorizationService.checkIfUserHasAccessToSourceApplication(sourceApplicationId)
    }

    fun authorizedIntegrationIds(): Set<Long> {
        val sourceApplicationIds =
            authorizationService.getUserAuthorizedSourceApplicationIds(
                integrationRepository.findDistinctSourceApplicationIds(),
            )
        return if (sourceApplicationIds.isEmpty()) {
            emptySet()
        } else {
            integrationRepository.findIdsBySourceApplicationIdIn(sourceApplicationIds)
        }
    }
}
