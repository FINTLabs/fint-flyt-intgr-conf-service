package no.novari.flyt.catalog.integration.validation.constraints

import no.novari.flyt.catalog.integration.validation.IntegrationValidationContext
import no.novari.flyt.catalog.validation.HibernateConstraintValidator
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext

class ReferencedConfigurationExistsValidator : HibernateConstraintValidator<ReferencedConfigurationExists, Long> {
    override fun isValid(
        value: Long?,
        hibernateConstraintValidatorContext: HibernateConstraintValidatorContext,
    ): Boolean {
        val integrationValidationContext =
            hibernateConstraintValidatorContext.getConstraintValidatorPayload(
                IntegrationValidationContext::class.java,
            )

        return value == null || integrationValidationContext?.configuration != null
    }
}
