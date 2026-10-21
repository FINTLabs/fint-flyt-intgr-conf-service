package no.novari.flyt.catalog.configuration.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Constraint(validatedBy = [AtLeastOneChildValidator::class])
annotation class AtLeastOneChild(
    val message: String = "has no children",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
