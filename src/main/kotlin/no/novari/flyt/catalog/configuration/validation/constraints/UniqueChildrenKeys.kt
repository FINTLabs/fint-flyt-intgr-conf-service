package no.novari.flyt.catalog.configuration.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Constraint(validatedBy = [UniqueChildrenKeysValidator::class])
annotation class UniqueChildrenKeys(
    val message: String = "contains duplicate children keys: {$DUPLICATE_CHILDREN_KEYS_REF}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
) {
    companion object {
        const val DUPLICATE_CHILDREN_KEYS_REF = "duplicateChildrenKeys"
    }
}
