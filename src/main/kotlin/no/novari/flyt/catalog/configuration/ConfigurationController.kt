package no.novari.flyt.catalog.configuration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import jakarta.validation.groups.Default
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationPageResponse
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationPatchDto
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidatorFactory
import no.novari.flyt.catalog.configuration.validation.ValidationErrorsFormattingService
import no.novari.flyt.catalog.configuration.validation.groups.Completed
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("$INTERNAL_API/konfigurasjoner")
@Tag(name = "Configurations", description = "Management of Flyt integration configurations.")
class ConfigurationController(
    private val configurationService: ConfigurationService,
    private val configurationValidatorFactory: ConfigurationValidatorFactory,
    private val validationErrorsFormattingService: ValidationErrorsFormattingService,
) {
    @GetMapping
    @Operation(summary = "List configurations")
    fun getConfigurations(
        @Parameter(description = "Zero-based page number")
        @RequestParam(name = "side") page: Int,
        @Parameter(description = "Number of configurations per page")
        @RequestParam(name = "antall") size: Int,
        @Parameter(description = "Property to sort by")
        @RequestParam(name = "sorteringFelt") sortProperty: String,
        @Parameter(description = "Sort direction")
        @RequestParam(name = "sorteringRetning") sortDirection: Sort.Direction,
        @Parameter(description = "Filter by integration identifier")
        @RequestParam(name = "integrasjonId", required = false) integrationId: Long?,
        @Parameter(description = "Filter by completion status")
        @RequestParam(name = "ferdigstilt", required = false) complete: Boolean?,
        @Parameter(description = "Exclude mapping content from responses")
        @RequestParam(name = "ekskluderMapping", required = false, defaultValue = "false") excludeMapping: Boolean,
    ): ConfigurationPageResponse {
        val filter = ConfigurationFilter(integrationId = integrationId, completed = complete)
        val pageRequest = PageRequest.of(page, size).withSort(sortDirection, sortProperty)

        val configurations = configurationService.findAll(filter, excludeMapping, pageRequest)

        return ConfigurationPageResponse(
            content = configurations.content,
            totalElements = configurations.totalElements,
            totalPages = configurations.totalPages,
        )
    }

    @GetMapping("{configurationId}")
    @Operation(summary = "Get a configuration")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Configuration found"),
            ApiResponse(responseCode = "404", description = "Configuration not found"),
        ],
    )
    fun getConfiguration(
        @Parameter(description = "Configuration identifier")
        @PathVariable configurationId: Long,
        @Parameter(description = "Exclude mapping content from the response")
        @RequestParam(name = "ekskluderMapping", required = false, defaultValue = "false") excludeMapping: Boolean,
    ): ConfigurationDto {
        return configurationService.findById(configurationId, excludeMapping)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PostMapping
    @Operation(summary = "Create a configuration")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Configuration created"),
            ApiResponse(responseCode = "422", description = "Invalid configuration"),
        ],
    )
    fun postConfiguration(
        @RequestBody configurationDto: ConfigurationDto,
    ): ConfigurationDto {
        validateBeanConstraints(
            requireNotNull(configurationDto.integrationId),
            requireNotNull(configurationDto.integrationMetadataId),
            configurationDto,
        )
        return configurationService.save(configurationDto)
    }

    @PatchMapping("{configurationId}")
    @Operation(summary = "Update a configuration")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Configuration updated"),
            ApiResponse(responseCode = "403", description = "Completed configuration cannot be changed"),
            ApiResponse(responseCode = "404", description = "Configuration not found"),
            ApiResponse(responseCode = "422", description = "Invalid configuration"),
        ],
    )
    fun patchConfiguration(
        @Parameter(description = "Configuration identifier")
        @PathVariable configurationId: Long,
        @RequestBody configurationPatchDto: ConfigurationPatchDto,
    ): ConfigurationDto {
        val configurationDto =
            configurationService.findById(configurationId, false)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        validateIsNotCompleted(configurationDto)

        val configurationDtoBuilder =
            configurationDto.toBuilder().apply {
                configurationPatchDto.integrationMetadataId?.let(this::integrationMetadataId)
                configurationPatchDto.completed?.takeIf { it }?.let(this::completed)
                configurationPatchDto.comment?.let(this::comment)
                configurationPatchDto.mapping?.let(this::mapping)
            }

        val newConfigurationDto = configurationDtoBuilder.build()

        validateBeanConstraints(
            requireNotNull(newConfigurationDto.integrationId),
            requireNotNull(newConfigurationDto.integrationMetadataId),
            newConfigurationDto,
        )

        return configurationService.updateById(configurationId, configurationPatchDto)
    }

    @DeleteMapping("{configurationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a configuration")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Configuration deleted"),
            ApiResponse(responseCode = "403", description = "Completed configuration cannot be deleted"),
            ApiResponse(responseCode = "404", description = "Configuration not found"),
        ],
    )
    fun deleteConfiguration(
        @Parameter(description = "Configuration identifier")
        @PathVariable configurationId: Long,
    ) {
        val configurationDto =
            configurationService.findById(configurationId, true)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        validateIsNotCompleted(configurationDto)
        configurationService.deleteById(configurationId)
    }

    private fun validateIsNotCompleted(configurationDto: ConfigurationDto) {
        if (configurationDto.completed) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Configuration is completed, and cannot be altered",
            )
        }
    }

    private fun validateBeanConstraints(
        integrationId: Long,
        metadataId: Long,
        configurationDto: ConfigurationDto,
    ) {
        val validator: Validator = configurationValidatorFactory.getValidator(integrationId, metadataId)
        val constraintViolations: Set<ConstraintViolation<ConfigurationDto>> =
            if (configurationDto.completed) {
                validator.validate(configurationDto, Default::class.java, Completed::class.java)
            } else {
                validator.validate(configurationDto, Default::class.java)
            }

        if (constraintViolations.isNotEmpty()) {
            throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                validationErrorsFormattingService.format(constraintViolations),
            )
        }
    }
}
