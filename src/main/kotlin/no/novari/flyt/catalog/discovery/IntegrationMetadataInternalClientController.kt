package no.novari.flyt.catalog.discovery

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.security.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/metadata")
class IntegrationMetadataInternalClientController(
    private val integrationMetadataService: IntegrationMetadataService,
) {
    @Operation(summary = "Get the latest metadata version for an integration as an internal client")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Metadata with the highest registered version number"),
            ApiResponse(
                responseCode = "400",
                description = "Missing or invalid query parameters",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", description = "Missing or invalid token", content = [Content()]),
            ApiResponse(responseCode = "403", description = "Client is not authorized", content = [Content()]),
            ApiResponse(
                responseCode = "404",
                description = "Metadata not found for the source and integration",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun getLatest(
        @RequestParam sourceApplicationId: Long,
        @RequestParam @NotBlank sourceApplicationIntegrationId: String,
    ): IntegrationMetadataDto =
        integrationMetadataService.getLatestForIntegration(sourceApplicationId, sourceApplicationIntegrationId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Integration metadata not found")

    @Operation(summary = "Register an integration metadata version as an internal client")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Metadata version stored", content = [Content()]),
            ApiResponse(responseCode = "400", description = "Invalid metadata request"),
            ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            ApiResponse(responseCode = "403", description = "Client is not authorized"),
            ApiResponse(responseCode = "409", description = "Metadata version already exists"),
        ],
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody integrationMetadata: IntegrationMetadataDto,
    ) = integrationMetadataService.register(integrationMetadata)
}
