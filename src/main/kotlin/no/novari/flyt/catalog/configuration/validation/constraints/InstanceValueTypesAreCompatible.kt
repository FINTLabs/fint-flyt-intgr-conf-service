package no.novari.flyt.catalog.configuration.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Constraint(validatedBy = [InstanceValueTypesAreCompatibleValidator::class])
annotation class InstanceValueTypesAreCompatible(
    val message: String =
        "contains references to instance values that are incompatible with configuration value type={$CONFIGURATION_VALUE_TYPE} : {$INCOMPATIBLE_INSTANCE_VALUES_KEY_AND_TYPE}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
) {
    companion object {
        const val CONFIGURATION_VALUE_TYPE = "configurationValueType"
        const val INCOMPATIBLE_INSTANCE_VALUES_KEY_AND_TYPE = "incompatibleInstanceValuesKeyAndType"
    }
}
