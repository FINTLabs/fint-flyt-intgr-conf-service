package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.validation.HibernateConstraintValidator
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext

class AtLeastOneChildValidator : HibernateConstraintValidator<AtLeastOneChild, ObjectMappingDto> {
    override fun isValid(
        value: ObjectMappingDto?,
        hibernateConstraintValidatorContext: HibernateConstraintValidatorContext,
    ): Boolean {
        return value == null ||
            value.valueMappingPerKey.isNotEmpty() ||
            value.valueCollectionMappingPerKey.isNotEmpty() ||
            value.objectMappingPerKey.isNotEmpty() ||
            value.objectCollectionMappingPerKey.isNotEmpty()
    }
}
