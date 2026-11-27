package no.novari.flyt.catalog.valueconverting.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.novari.flyt.catalog.security.AuthorizationService
import no.novari.flyt.catalog.valueconverting.api.dto.ValueConversionFilterParams
import no.novari.flyt.catalog.valueconverting.api.dto.ValueConversionPageResponse
import no.novari.flyt.catalog.valueconverting.api.dto.ValueConversionRequest
import no.novari.flyt.catalog.valueconverting.api.dto.ValueConversionResponse
import no.novari.flyt.catalog.valueconverting.api.exception.ValueConversionNotFoundException
import no.novari.flyt.catalog.valueconverting.application.ValueConversionService
import no.novari.flyt.security.UrlPaths.INTERNAL_API
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("$INTERNAL_API/value-convertings")
@Tag(name = "Value conversions", description = "Administrasjon av verdikonverteringer mellom applikasjoner.")
class ValueConversionController(
    private val valueConversionService: ValueConversionService,
    private val authorizationService: AuthorizationService,
) {
    @GetMapping
    @Operation(summary = "Hent verdikonverteringer")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "En side med verdikonverteringer."),
                ApiResponse(responseCode = "400", description = "Ugyldige filter- eller sideparametere."),
                ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering."),
                ApiResponse(responseCode = "403", description = "Brukeren har ikke tilgang."),
            ],
    )
    fun getValueConversions(
        @Valid @ModelAttribute filterParams: ValueConversionFilterParams,
    ): ValueConversionPageResponse {
        val pageRequest = filterParams.toPageRequest()
        val filter = filterParams.toFilter()

        val sourceApplicationIds =
            authorizationService.getUserAuthorizedSourceApplicationIds(
                valueConversionService.findDistinctSourceApplicationIds(),
            )

        val valueConversions =
            valueConversionService.findAllBySourceApplicationIds(
                pageable = pageRequest,
                includeConversionMap = !filterParams.excludeConvertingMap,
                authorizedSourceApplicationIds = sourceApplicationIds,
                filter = filter,
            )

        return ValueConversionPageResponse.from(valueConversions)
    }

    @GetMapping("{valueConversionId}")
    @Operation(summary = "Hent en verdikonvertering")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "Verdikonverteringen."),
                ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering."),
                ApiResponse(responseCode = "403", description = "Brukeren har ikke tilgang."),
                ApiResponse(responseCode = "404", description = "Verdikonverteringen finnes ikke."),
            ],
    )
    fun getValueConversion(
        @Parameter(description = "ID-en til verdikonverteringen.", example = "42")
        @PathVariable
        valueConversionId: Long,
    ): ValueConversionResponse {
        val valueConversion =
            valueConversionService.findById(valueConversionId)
                ?: throw ValueConversionNotFoundException(valueConversionId)

        authorizationService.checkIfUserHasAccessToSourceApplication(valueConversion.fromApplicationId)

        return valueConversion
    }

    @PostMapping
    @Operation(summary = "Opprett en verdikonvertering")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "Verdikonverteringen ble opprettet."),
                ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering."),
                ApiResponse(responseCode = "403", description = "Brukeren har ikke tilgang."),
                ApiResponse(responseCode = "422", description = "Forespørselen kunne ikke valideres."),
            ],
    )
    fun postValueConversion(
        @Valid @RequestBody valueConversionRequest: ValueConversionRequest,
    ): ValueConversionResponse {
        authorizationService.checkIfUserHasAccessToSourceApplication(valueConversionRequest.fromApplicationId)

        return valueConversionService.save(valueConversionRequest)
    }

    @PutMapping("{valueConversionId}")
    @Operation(summary = "Oppdater en verdikonvertering")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "Verdikonverteringen ble oppdatert."),
                ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering."),
                ApiResponse(responseCode = "403", description = "Brukeren har ikke tilgang."),
                ApiResponse(responseCode = "404", description = "Verdikonverteringen finnes ikke."),
                ApiResponse(responseCode = "422", description = "Forespørselen kunne ikke valideres."),
            ],
    )
    fun putValueConversion(
        @Parameter(description = "ID-en til verdikonverteringen.", example = "42")
        @PathVariable
        valueConversionId: Long,
        @Valid @RequestBody valueConversionRequest: ValueConversionRequest,
    ): ValueConversionResponse {
        val valueConversion =
            valueConversionService.findById(valueConversionId)
                ?: throw ValueConversionNotFoundException(valueConversionId)

        authorizationService.checkIfUserHasAccessToSourceApplication(valueConversion.fromApplicationId)

        return valueConversionService.update(valueConversionId, valueConversionRequest)
    }

    @DeleteMapping("{valueConversionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Slett en verdikonvertering")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "204", description = "Verdikonverteringen ble slettet."),
                ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering."),
                ApiResponse(responseCode = "403", description = "Brukeren har ikke tilgang."),
                ApiResponse(responseCode = "404", description = "Verdikonverteringen finnes ikke."),
            ],
    )
    fun deleteValueConversion(
        @Parameter(description = "ID-en til verdikonverteringen.", example = "42")
        @PathVariable
        valueConversionId: Long,
    ) {
        val valueConversion =
            valueConversionService.findById(valueConversionId)
                ?: throw ValueConversionNotFoundException(valueConversionId)

        authorizationService.checkIfUserHasAccessToSourceApplication(valueConversion.fromApplicationId)

        valueConversionService.delete(valueConversionId)
    }
}
