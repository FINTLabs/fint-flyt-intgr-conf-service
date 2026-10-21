package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.valueparsability.ValueParsabilityValidator
import org.springframework.stereotype.Service

@Service
class BooleanParsabilityValidator : ValueParsabilityValidator {
    override fun getTypeToValidate(): ValueMapping.Type = ValueMapping.Type.BOOLEAN

    override fun isValid(value: String): Boolean = value == "true" || value == "false"
}
