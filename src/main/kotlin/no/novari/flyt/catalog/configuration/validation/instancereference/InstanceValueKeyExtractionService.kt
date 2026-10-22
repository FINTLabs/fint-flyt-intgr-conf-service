package no.novari.flyt.catalog.configuration.validation.instancereference

import org.springframework.stereotype.Service
import java.util.regex.MatchResult
import java.util.regex.Pattern

@Service
class InstanceValueKeyExtractionService {
    private val referenceExtractionPattern = Pattern.compile("${Pattern.quote(IF_REFERENCE_PREFIX)}[^}]+}")

    fun extractIfReferenceKeys(value: String?): Collection<String> =
        value
            ?.let { content ->
                referenceExtractionPattern
                    .matcher(content)
                    .results()
                    .map(MatchResult::group)
                    .map { ifReference -> ifReference.removePrefix(IF_REFERENCE_PREFIX).removeSuffix("}") }
                    .toList()
            }
            ?: emptyList()

    private companion object {
        const val IF_REFERENCE_PREFIX = "\$if{"
    }
}
