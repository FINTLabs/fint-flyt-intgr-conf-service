package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.valueparsability.ValueParsabilityValidator
import org.springframework.stereotype.Service
import java.net.URI

@Service
class UrlParsabilityValidator : ValueParsabilityValidator {
    override fun getTypeToValidate(): ValueMapping.Type = ValueMapping.Type.URL

    override fun isValid(value: String): Boolean =
        try {
            URI.create(value).toURL()
            true
        } catch (_: Exception) {
            false
        }
}
