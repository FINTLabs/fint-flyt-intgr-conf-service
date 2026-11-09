package no.novari.flyt.catalog.integration.api

import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.web.NotFoundException
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/integrasjoner")
class IntegrationInternalClientController(
    private val integrationService: IntegrationService,
) {
    @GetMapping
    fun getIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
        @RequestParam sourceApplicationId: Long,
        @RequestParam sourceApplicationIntegrationId: String,
    ): IntegrationDto =
        integrationService.findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
            sourceApplicationId,
            sourceApplicationIntegrationId,
        ) ?: throw NotFoundException()

    @GetMapping("{integrationId}/aktiv-konfigurasjon-id")
    fun getActiveConfigurationId(
        @PathVariable integrationId: Long,
    ): Long = integrationService.findActiveConfigurationIdByIntegrationId(integrationId) ?: throw NotFoundException()
}
