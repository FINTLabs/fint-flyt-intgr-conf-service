package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.validation.ConfigurationValidationContext
import no.novari.flyt.catalog.validation.HibernateConstraintValidator

class IntegrationAndMetadataMatchesValidator : HibernateConstraintValidator<IntegrationAndMetadataMatches, Any> {
    override fun isValid(
        value: Any?,
        hibernateConstraintValidatorContext:
            org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext,
    ): Boolean {
        val configurationValidationContext =
            hibernateConstraintValidatorContext
                .getConstraintValidatorPayload(ConfigurationValidationContext::class.java)

        val integration = configurationValidationContext.integration
        val metadata = configurationValidationContext.metadata

        return integration?.sourceApplicationId == metadata?.sourceApplicationId &&
            integration?.sourceApplicationIntegrationId == metadata?.sourceApplicationIntegrationId
    }
}
