package no.novari.flyt.catalog.integration.validation

import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import no.novari.flyt.catalog.integration.kafka.ConfigurationRequestProducerService
import org.hibernate.validator.HibernateValidatorFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class IntegrationValidatorFactory(
    @Qualifier("defaultValidator") private val validatorFactory: ValidatorFactory,
    private val configurationRequestProducerService: ConfigurationRequestProducerService,
) {
    fun getPatchValidator(
        integrationId: Long,
        configurationId: Long?,
    ): Validator {
        return validatorFactory
            .unwrap(HibernateValidatorFactory::class.java)
            .usingContext()
            .constraintValidatorPayload(getConfigurationValidationContext(integrationId, configurationId))
            .validator
    }

    private fun getConfigurationValidationContext(
        integrationId: Long,
        configurationId: Long?,
    ): IntegrationValidationContext {
        return IntegrationValidationContext(
            integrationId = integrationId,
            configuration = configurationId?.let(configurationRequestProducerService::get),
        )
    }
}
