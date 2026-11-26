package no.novari.flyt.catalog.discovery

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/metadata")
class IntegrationMetadataInternalClientController(
    private val integrationMetadataService: IntegrationMetadataService,
) {
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
