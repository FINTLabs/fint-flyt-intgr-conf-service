package no.novari.flyt.catalog.integration.validation.constraints

import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.catalog.integration.validation.IntegrationValidationContext
import no.novari.flyt.catalog.validation.HibernateConstraintValidator
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext

class ActiveConfigurationIsDefinedIfStateIsActiveValidator :
    HibernateConstraintValidator<ActiveConfigurationIsDefinedIfStateIsActive, Integration.State> {
    override fun isValid(
        value: Integration.State?,
        hibernateConstraintValidatorContext: HibernateConstraintValidatorContext,
    ): Boolean {
        return value != Integration.State.ACTIVE ||
            hibernateConstraintValidatorContext
                .getConstraintValidatorPayload(IntegrationValidationContext::class.java)
                ?.configuration != null
    }
}
