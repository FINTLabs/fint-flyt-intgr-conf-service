package no.novari.flyt.catalog.configuration

import io.swagger.v3.oas.annotations.tags.Tag
import no.novari.flyt.audit.web.HistoryControllerSupport
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationSnapshot
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$INTERNAL_API/konfigurasjoner")
@Tag(name = "Configuration history", description = "Audit history for Flyt configurations.")
class ConfigurationHistoryController(
    historyService: ConfigurationHistoryService,
) : HistoryControllerSupport<Configuration, Long, ConfigurationSnapshot>(historyService)
