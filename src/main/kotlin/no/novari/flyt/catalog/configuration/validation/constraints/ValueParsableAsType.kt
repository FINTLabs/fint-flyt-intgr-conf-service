package no.novari.flyt.catalog.configuration.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Constraint(validatedBy = [ValueParsableAsTypeValidator::class])
annotation class ValueParsableAsType(
    val message: String = "contains value that is not parsable as type={$VALUE_TYPE}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
) {
    companion object {
        const val VALUE_TYPE = "valueType"
    }
}
