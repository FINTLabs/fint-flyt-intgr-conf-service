package no.novari.flyt.catalog.discovery

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import no.novari.flyt.catalog.validation.ValidationErrorsFormattingService
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RequestMapping("$INTERNAL_API/metadata")
@RestController
@Tag(name = "Integration metadata", description = "Metadata definitions for Flyt integrations and instances.")
class IntegrationMetadataController(
    private val integrationMetadataService: IntegrationMetadataService,
    private val validator: Validator,
    private val validationErrorsFormattingService: ValidationErrorsFormattingService,
    private val userAuthorizationService: UserAuthorizationService,
) {
    @GetMapping(params = ["kildeapplikasjonId", "kildeapplikasjonIntegrasjonId", "bareSisteVersjoner"])
    @Hidden
    fun getIntegrationMetadataForSourceApplication(): ResponseEntity<Collection<IntegrationMetadata>> =
        ResponseEntity
            .badRequest()
            .build()

    @GetMapping(params = ["kildeapplikasjonId"])
    @Operation(summary = "List metadata for a source application", operationId = "getSourceApplicationMetadata")
    fun getIntegrationMetadataForSourceApplication(
        authentication: Authentication,
        @Parameter(description = "Source application identifier")
        @RequestParam(name = "kildeapplikasjonId") sourceApplicationId: Long,
        @Parameter(description = "Return only the latest version for each integration")
        @RequestParam(name = "bareSisteVersjoner", required = false) onlyLatestVersions: Boolean?,
    ): ResponseEntity<Collection<IntegrationMetadataDto>> {
        userAuthorizationService.checkIfUserHasAccessToSourceApplication(authentication, sourceApplicationId)

        val integrationMetadata =
            integrationMetadataService.getIntegrationMetadataForSourceApplication(
                sourceApplicationId = sourceApplicationId,
                onlyLatestVersions = onlyLatestVersions ?: false,
            )

        return ResponseEntity.ok(integrationMetadata)
    }

    @GetMapping(params = ["kildeapplikasjonIds"])
    @Operation(
        summary = "List metadata for multiple source applications",
        operationId = "getSourceApplicationsMetadata",
    )
    fun getIntegrationMetadataForSourceApplications(
        authentication: Authentication,
        @Parameter(description = "Source application identifiers")
        @RequestParam(name = "kildeapplikasjonIds") sourceApplicationIds: Collection<Long>,
        @Parameter(description = "Return only the latest version for each integration")
        @RequestParam(name = "bareSisteVersjoner", required = false) onlyLatestVersions: Boolean?,
    ): ResponseEntity<Map<Long, Collection<IntegrationMetadataDto>>> {
        val requestedSourceApplicationIds = sourceApplicationIds.toSet()
        val authorizedSourceApplicationIds =
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                requestedSourceApplicationIds,
            )
        if (!authorizedSourceApplicationIds.containsAll(requestedSourceApplicationIds)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        val integrationMetadata =
            integrationMetadataService.getIntegrationMetadataForSourceApplications(
                sourceApplicationIds = sourceApplicationIds,
                onlyLatestVersions = onlyLatestVersions ?: false,
            )

        return ResponseEntity.ok(integrationMetadata)
    }

    @GetMapping(params = ["kildeapplikasjonId", "kildeapplikasjonIntegrasjonId"])
    @Operation(summary = "List metadata versions for an integration", operationId = "getIntegrationMetadata")
    fun getIntegrationMetadataForIntegration(
        authentication: Authentication,
        @Parameter(description = "Source application identifier")
        @RequestParam(name = "kildeapplikasjonId") sourceApplicationId: Long,
        @Parameter(description = "Source application integration identifier")
        @RequestParam(name = "kildeapplikasjonIntegrasjonId") sourceApplicationIntegrationId: String,
    ): ResponseEntity<Collection<IntegrationMetadataDto>> {
        userAuthorizationService.checkIfUserHasAccessToSourceApplication(authentication, sourceApplicationId)

        val integrationMetadata =
            integrationMetadataService.getAllForSourceApplicationIdAndSourceApplicationIntegrationId(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationIntegrationId = sourceApplicationIntegrationId,
            )

        return ResponseEntity.ok(integrationMetadata)
    }

    @GetMapping("{metadataId}/instans-metadata")
    @Operation(summary = "Get instance metadata by integration metadata identifier")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Instance metadata found"),
            ApiResponse(responseCode = "403", description = "Source application access denied"),
            ApiResponse(responseCode = "404", description = "Integration metadata not found"),
        ],
    )
    fun getInstanceElementMetadataForIntegrationMetadataWithId(
        authentication: Authentication,
        @Parameter(description = "Integration metadata identifier")
        @PathVariable metadataId: Long,
    ): ResponseEntity<InstanceMetadataContentDto?> {
        val integrationMetadataDto =
            integrationMetadataService.getById(metadataId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            requireNotNull(integrationMetadataDto.sourceApplicationId),
        )

        return ResponseEntity.ok(integrationMetadataDto.instanceMetadata)
    }

    @PostMapping
    @Operation(summary = "Publish an integration metadata version")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Metadata version published"),
            ApiResponse(responseCode = "403", description = "Source application access denied"),
            ApiResponse(responseCode = "409", description = "Metadata version already exists"),
            ApiResponse(responseCode = "422", description = "Invalid metadata"),
        ],
    )
    fun post(
        authentication: Authentication,
        @RequestBody integrationMetadataDto: IntegrationMetadataDto,
    ): ResponseEntity<Void> {
        val constraintViolations: Set<ConstraintViolation<IntegrationMetadataDto>> =
            validator.validate(
                integrationMetadataDto,
            )
        if (constraintViolations.isNotEmpty()) {
            val formattedErrors = validationErrorsFormattingService.format(constraintViolations)
            log.atWarn {
                message =
                    "Rejected integration metadata request because validation failed. " +
                    "sourceApplicationId={}, sourceApplicationIntegrationId={}, version={}, errors={}"
                arguments =
                    arrayOf(
                        integrationMetadataDto.sourceApplicationId,
                        integrationMetadataDto.sourceApplicationIntegrationId,
                        integrationMetadataDto.version,
                        formattedErrors,
                    )
            }
            throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                formattedErrors,
            )
        }

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            requireNotNull(integrationMetadataDto.sourceApplicationId),
        )

        if (integrationMetadataService.versionExists(integrationMetadataDto)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Version already exists")
        }

        integrationMetadataService.save(integrationMetadataDto)
        return ResponseEntity.ok().build()
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
