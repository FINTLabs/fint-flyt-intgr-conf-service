package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.validation.valueparsability.ValueParsabilityValidator
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.springframework.stereotype.Service

@Service
class ValueParsableAsTypeValidator(
    private val valueParsabilityValidators: Collection<ValueParsabilityValidator>,
) : HibernateConstraintValidator<ValueParsableAsType, ValueMappingDto> {
    override fun isValid(
        value: ValueMappingDto?,
        hibernateConstraintValidatorContext: HibernateConstraintValidatorContext,
    ): Boolean {
        if (value == null) {
            return true
        }

        val valid = valueParsabilityValidators.all { it.isValid(value) }
        if (!valid) {
            hibernateConstraintValidatorContext.addMessageParameter(
                ValueParsableAsType.VALUE_TYPE,
                value.type.toString(),
            )
        }
        return valid
    }
}
