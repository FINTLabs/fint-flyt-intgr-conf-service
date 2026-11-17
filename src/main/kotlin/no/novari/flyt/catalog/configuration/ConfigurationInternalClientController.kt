package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/konfigurasjoner")
class ConfigurationInternalClientController(
    private val configurationService: ConfigurationService,
) {
    @GetMapping("{configurationId}/mapping")
    fun getMapping(
        @PathVariable configurationId: Long,
    ): ObjectMappingDto =
        configurationService.findById(configurationId, false)?.mapping
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}
