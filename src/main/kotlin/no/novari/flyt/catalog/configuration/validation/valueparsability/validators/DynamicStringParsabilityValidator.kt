package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.valueparsability.ValueParsabilityValidator
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class DynamicStringParsabilityValidator : ValueParsabilityValidator {
    private val textPattern = Pattern.compile("(?:(?!${Pattern.quote(IF_REFERENCE_PREFIX)}).)*")
    private val instanceValueKeyPattern = Pattern.compile("(?:(?!${Pattern.quote(IF_REFERENCE_PREFIX)}).)+")
    private val ifReferencePattern =
        Pattern.compile(
            "(?:${Pattern.quote(IF_REFERENCE_PREFIX)}$instanceValueKeyPattern\\})*",
        )
    private val dynamicStringPattern = Pattern.compile("^(?:$textPattern|$ifReferencePattern)*$")

    override fun getTypeToValidate(): ValueMapping.Type = ValueMapping.Type.DYNAMIC_STRING

    override fun isValid(value: String): Boolean = dynamicStringPattern.matcher(value).matches()

    private companion object {
        const val IF_REFERENCE_PREFIX = "\$if{"
    }
}
