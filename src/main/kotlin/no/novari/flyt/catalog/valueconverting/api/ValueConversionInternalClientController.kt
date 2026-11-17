package no.novari.flyt.catalog.valueconverting.api

import no.novari.flyt.catalog.valueconverting.domain.ValueConversion
import no.novari.flyt.catalog.valueconverting.infrastructure.persistence.ValueConversionRepository
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Returns the entity rather than `ValueConversionResponse`, since that is the shape the Kafka reply
 * on `value-converting` has always had.
 */
@RestController
@RequestMapping("$INTERNAL_CLIENT_API/value-convertings")
class ValueConversionInternalClientController(
    private val valueConversionRepository: ValueConversionRepository,
) {
    @GetMapping("{valueConvertingId}")
    fun getValueConversion(
        @PathVariable valueConvertingId: Long,
    ): ValueConversion =
        valueConversionRepository.findByIdOrNull(valueConvertingId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}
