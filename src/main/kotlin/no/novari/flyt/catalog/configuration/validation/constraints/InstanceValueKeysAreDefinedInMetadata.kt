package no.novari.flyt.catalog.configuration.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Constraint(validatedBy = [InstanceValueKeysAreDefinedInMetadataValidator::class])
annotation class InstanceValueKeysAreDefinedInMetadata(
    val message: String =
        "contains references to instance values that are not defined in the metadata: {$KEYS_MISSING_IN_METADATA}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
) {
    companion object {
        const val KEYS_MISSING_IN_METADATA = "keysMissingInMetadata"
    }
}
