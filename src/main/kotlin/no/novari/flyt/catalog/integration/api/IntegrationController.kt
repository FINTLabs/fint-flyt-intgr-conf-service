package no.novari.flyt.catalog.integration.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPageResponse
import no.novari.flyt.catalog.integration.api.dto.IntegrationPatchDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.application.IntegrationUpdateValidationService
import no.novari.flyt.catalog.integration.web.ConflictException
import no.novari.flyt.catalog.integration.web.ForbiddenWithoutBodyException
import no.novari.flyt.catalog.integration.web.NotFoundException
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$INTERNAL_API/integrasjoner")
@Tag(name = "Integrations", description = "Management of Flyt integration definitions.")
class IntegrationController(
    private val integrationService: IntegrationService,
    private val integrationUpdateValidationService: IntegrationUpdateValidationService,
    private val userAuthorizationService: UserAuthorizationService,
) {
    @GetMapping
    @Operation(summary = "List integrations", operationId = "listIntegrations")
    fun listIntegrations(
        authentication: Authentication,
        @Parameter(description = "Filter by source application identifier")
        @RequestParam(required = false) sourceApplicationId: Long?,
    ): Collection<IntegrationDto> {
        val sourceApplicationIds = resolveAuthorizedSourceApplicationIds(authentication, sourceApplicationId)
        return integrationService.findAllBySourceApplicationIds(sourceApplicationIds)
    }

    @GetMapping(params = ["side", "antall", "sorteringFelt", "sorteringRetning"])
    @Operation(summary = "List integrations with pagination", operationId = "listIntegrationsPaginated")
    fun listIntegrationsPaginated(
        authentication: Authentication,
        @Parameter(description = "Zero-based page number")
        @RequestParam(name = "side") page: Int,
        @Parameter(description = "Number of integrations per page")
        @RequestParam(name = "antall") size: Int,
        @Parameter(description = "Property to sort by")
        @RequestParam(name = "sorteringFelt") sortProperty: String,
        @Parameter(description = "Sort direction")
        @RequestParam(name = "sorteringRetning") sortDirection: Sort.Direction,
        @Parameter(description = "Filter by source application identifier")
        @RequestParam(required = false) sourceApplicationId: Long?,
    ): IntegrationPageResponse {
        val sourceApplicationIds = resolveAuthorizedSourceApplicationIds(authentication, sourceApplicationId)
        val pageRequest = PageRequest.of(page, size).withSort(sortDirection, sortProperty)
        val integrations = integrationService.findAllBySourceApplicationIds(sourceApplicationIds, pageRequest)
        return IntegrationPageResponse(
            content = integrations.content,
            totalElements = integrations.totalElements,
            totalPages = integrations.totalPages,
        )
    }

    @GetMapping("{integrationId}")
    @Operation(summary = "Get an integration")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Integration found"),
            ApiResponse(responseCode = "403", description = "Source application access denied"),
            ApiResponse(responseCode = "404", description = "Integration not found"),
        ],
    )
    fun getIntegrationById(
        authentication: Authentication,
        @Parameter(description = "Integration identifier")
        @PathVariable integrationId: Long,
    ): IntegrationDto {
        val integration =
            integrationService.findById(integrationId)
                ?: throw NotFoundException()

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            integration.sourceApplicationId,
        )

        return integration
    }

    @PostMapping
    @Operation(summary = "Create an integration")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Integration created"),
            ApiResponse(responseCode = "403", description = "Source application access denied"),
            ApiResponse(responseCode = "409", description = "Integration already exists"),
            ApiResponse(responseCode = "422", description = "Invalid integration"),
        ],
    )
    fun createIntegration(
        authentication: Authentication,
        @Valid @RequestBody integrationPostDto: IntegrationPostDto,
    ): IntegrationDto {
        ensureIntegrationDoesNotAlreadyExist(integrationPostDto)

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            requireNotNull(integrationPostDto.sourceApplicationId),
        )

        return integrationService.save(integrationPostDto)
    }

    @PatchMapping("{integrationId}")
    @Operation(summary = "Update an integration")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Integration updated"),
            ApiResponse(responseCode = "403", description = "Source application access denied"),
            ApiResponse(responseCode = "404", description = "Integration not found"),
            ApiResponse(responseCode = "422", description = "Invalid integration"),
        ],
    )
    fun updateIntegration(
        authentication: Authentication,
        @Parameter(description = "Integration identifier")
        @PathVariable integrationId: Long,
        @RequestBody integrationPatchDto: IntegrationPatchDto,
    ): IntegrationDto {
        val existingIntegration =
            integrationService.findById(integrationId)
                ?: throw NotFoundException()

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            existingIntegration.sourceApplicationId,
        )

        val updatedIntegration =
            existingIntegration.copy(
                destination = integrationPatchDto.destination ?: existingIntegration.destination,
                state = integrationPatchDto.state ?: existingIntegration.state,
                activeConfigurationId =
                    integrationPatchDto.activeConfigurationId ?: existingIntegration.activeConfigurationId,
            )

        integrationUpdateValidationService.validate(integrationId, updatedIntegration)

        return integrationService.updateById(integrationId, integrationPatchDto)
    }

    private fun resolveAuthorizedSourceApplicationIds(
        authentication: Authentication,
        sourceApplicationId: Long?,
    ): Set<Long> {
        val candidateSourceApplicationIds =
            sourceApplicationId?.let(::setOf) ?: integrationService.findDistinctSourceApplicationIds()
        val authorizedSourceApplicationIds =
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                candidateSourceApplicationIds,
            )

        if (sourceApplicationId != null && !authorizedSourceApplicationIds.contains(sourceApplicationId)) {
            throw ForbiddenWithoutBodyException()
        }

        return authorizedSourceApplicationIds
    }

    private fun ensureIntegrationDoesNotAlreadyExist(integrationPostDto: IntegrationPostDto) {
        if (
            integrationService.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                requireNotNull(integrationPostDto.sourceApplicationId),
                requireNotNull(integrationPostDto.sourceApplicationIntegrationId),
            )
        ) {
            throw ConflictException()
        }
    }
}
