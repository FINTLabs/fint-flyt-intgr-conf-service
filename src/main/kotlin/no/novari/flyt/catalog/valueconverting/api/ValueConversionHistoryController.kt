package no.novari.flyt.catalog.valueconverting.api

import io.swagger.v3.oas.annotations.tags.Tag
import no.novari.flyt.audit.history.AuditPropertyFilter
import no.novari.flyt.audit.web.HistoryControllerSupport
import no.novari.flyt.catalog.security.AuthorizationService
import no.novari.flyt.catalog.valueconverting.api.dto.ValueConversionSnapshot
import no.novari.flyt.catalog.valueconverting.api.exception.ValueConversionNotFoundException
import no.novari.flyt.catalog.valueconverting.domain.ValueConversion
import no.novari.flyt.catalog.valueconverting.domain.ValueConversionHistoryService
import no.novari.flyt.catalog.valueconverting.infrastructure.persistence.ValueConversionRepository
import no.novari.flyt.security.UrlPaths.INTERNAL_API
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$INTERNAL_API/value-convertings")
@Tag(name = "Value conversion history", description = "Revisjonshistorikk for verdikonverteringer.")
class ValueConversionHistoryController(
    private val valueConversionRepository: ValueConversionRepository,
    private val authorizationService: AuthorizationService,
    historyService: ValueConversionHistoryService,
) : HistoryControllerSupport<ValueConversion, Long, ValueConversionSnapshot>(historyService) {
    public override fun checkAccess(
        authentication: Authentication,
        id: Long,
    ) {
        val valueConversion =
            valueConversionRepository
                .findByIdOrNull(id)
                ?: throw ValueConversionNotFoundException(id)
        authorizationService.checkIfUserHasAccessToSourceApplication(checkNotNull(valueConversion.fromApplicationId))
    }

    public override fun additionalFilter(authentication: Authentication): AuditPropertyFilter =
        AuditPropertyFilter(
            property = "fromApplicationId",
            allowedValues =
                authorizationService.getUserAuthorizedSourceApplicationIds(
                    valueConversionRepository.findDistinctSourceApplicationIds(),
                ),
        )
}
