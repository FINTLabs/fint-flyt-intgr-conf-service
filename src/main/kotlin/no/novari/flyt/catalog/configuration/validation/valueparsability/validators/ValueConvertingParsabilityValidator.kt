package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.valueparsability.ValueParsabilityValidator
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class ValueConvertingParsabilityValidator : ValueParsabilityValidator {
    private val numberPattern = Pattern.compile("\\d+")
    private val valueConverterReferencePattern =
        Pattern.compile(
            "${Pattern.quote(VALUE_CONVERTER_PREFIX)}$numberPattern\\}",
        )

    private val instanceValueKeyPattern = Pattern.compile("(?:(?!${Pattern.quote(IF_REFERENCE_PREFIX)}).)+")
    private val ifReferencePattern = Pattern.compile("${Pattern.quote(IF_REFERENCE_PREFIX)}$instanceValueKeyPattern\\}")
    private val instanceCollectionFieldReferencePattern =
        Pattern.compile(
            "${Pattern.quote(INSTANCE_COLLECTION_FIELD_PREFIX)}$numberPattern\\}\\{$instanceValueKeyPattern\\}",
        )

    private val valueConvertingPattern =
        Pattern.compile(
            "^$valueConverterReferencePattern($ifReferencePattern|$instanceCollectionFieldReferencePattern)$",
        )

    override fun getTypeToValidate(): ValueMapping.Type = ValueMapping.Type.VALUE_CONVERTING

    override fun isValid(value: String): Boolean = valueConvertingPattern.matcher(value).matches()

    private companion object {
        const val VALUE_CONVERTER_PREFIX = "\$vc{"
        const val IF_REFERENCE_PREFIX = "\$if{"
        const val INSTANCE_COLLECTION_FIELD_PREFIX = "\$icf{"
    }
}
