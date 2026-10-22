package no.novari.flyt.catalog.integration.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ActiveConfigurationIsDefinedIfStateIsActiveValidator::class])
annotation class ActiveConfigurationIsDefinedIfStateIsActive(
    val message: String = "is cannot be set to ACTIVE without an active configuration",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
