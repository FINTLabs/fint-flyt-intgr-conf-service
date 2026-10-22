package no.novari.flyt.catalog.configuration.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Constraint(validatedBy = [IntegrationAndMetadataMatchesValidator::class])
annotation class IntegrationAndMetadataMatches(
    val message: String =
        "Integration and metadata do not have matching source application ids and/or source application integration ids",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
