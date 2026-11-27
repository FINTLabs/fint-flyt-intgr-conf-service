package no.novari.flyt.catalog.configuration

import io.swagger.v3.oas.annotations.tags.Tag
import no.novari.flyt.audit.history.AuditPropertyFilter
import no.novari.flyt.audit.web.HistoryControllerSupport
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationSnapshot
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import no.novari.flyt.security.UrlPaths.INTERNAL_API
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("$INTERNAL_API/konfigurasjoner")
@Tag(name = "Configuration history", description = "Audit history for Flyt configurations.")
class ConfigurationHistoryController(
    private val configurationRepository: ConfigurationRepository,
    private val configurationAccess: ConfigurationAccess,
    historyService: ConfigurationHistoryService,
) : HistoryControllerSupport<Configuration, Long, ConfigurationSnapshot>(historyService) {
    public override fun checkAccess(
        authentication: Authentication,
        id: Long,
    ) {
        val configuration =
            configurationRepository.findByIdOrNull(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        configurationAccess.checkAccessToIntegration(checkNotNull(configuration.integrationId))
    }

    // The audited configuration has no source application, so the history is filtered on its integration.
    public override fun additionalFilter(authentication: Authentication): AuditPropertyFilter =
        AuditPropertyFilter(property = "integrationId", allowedValues = configurationAccess.authorizedIntegrationIds())
}
