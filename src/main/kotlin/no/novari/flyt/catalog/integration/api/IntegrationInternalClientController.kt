package no.novari.flyt.catalog.integration.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    @Operation(summary = "Look up an integration and its state as an internal client")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Integration found with state ACTIVE or DEACTIVATED"),
            ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            ApiResponse(responseCode = "403", description = "Client is not authorized"),
            ApiResponse(responseCode = "404", description = "Integration not found"),
        ],
    )
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
