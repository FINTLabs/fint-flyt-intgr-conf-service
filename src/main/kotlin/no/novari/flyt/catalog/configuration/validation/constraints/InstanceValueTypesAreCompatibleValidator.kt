package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidationContext
import no.novari.flyt.catalog.configuration.validation.instancereference.InstanceValueKeyExtractionService
import no.novari.flyt.catalog.configuration.validation.instancereference.InstanceValueTypeCompatibilityValidator
import no.novari.flyt.catalog.validation.HibernateConstraintValidator
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.springframework.stereotype.Service

@Service
class InstanceValueTypesAreCompatibleValidator(
    private val instanceValueKeyExtractionService: InstanceValueKeyExtractionService,
    private val instanceValueTypeCompatibilityValidators: Collection<InstanceValueTypeCompatibilityValidator>,
) : HibernateConstraintValidator<InstanceValueTypesAreCompatible, ValueMappingDto> {
    override fun isValid(
        value: ValueMappingDto?,
        hibernateConstraintValidatorContext: HibernateConstraintValidatorContext,
    ): Boolean {
        if (value == null) {
            return true
        }

        val referencedInstanceValueKeys =
            instanceValueKeyExtractionService.extractIfReferenceKeys(value.mappingString)

        val metadataInstanceValueTypePerKey =
            hibernateConstraintValidatorContext
                .getConstraintValidatorPayload(ConfigurationValidationContext::class.java)
                .instanceValueTypePerKey

        val incompatibleInstanceValuesKeyAndType =
            instanceValueTypeCompatibilityValidators
                .flatMap { compatibilityValidator ->
                    compatibilityValidator.findIncompatibleInstanceValuesKeyAndType(
                        value,
                        referencedInstanceValueKeys,
                        metadataInstanceValueTypePerKey,
                    )
                }

        if (incompatibleInstanceValuesKeyAndType.isNotEmpty()) {
            hibernateConstraintValidatorContext.addMessageParameter(
                InstanceValueTypesAreCompatible.CONFIGURATION_VALUE_TYPE,
                value.type?.name,
            )
            hibernateConstraintValidatorContext.addMessageParameter(
                InstanceValueTypesAreCompatible.INCOMPATIBLE_INSTANCE_VALUES_KEY_AND_TYPE,
                incompatibleInstanceValuesKeyAndType.joinToString(prefix = "[", postfix = "]") { (key, type) ->
                    "{key=$key, type=$type}"
                },
            )
        }
        return incompatibleInstanceValuesKeyAndType.isEmpty()
    }
}
