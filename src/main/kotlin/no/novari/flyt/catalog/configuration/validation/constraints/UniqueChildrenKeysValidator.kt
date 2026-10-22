package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.validation.HibernateConstraintValidator
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.hibernate.validator.internal.util.CollectionHelper

class UniqueChildrenKeysValidator : HibernateConstraintValidator<UniqueChildrenKeys, ObjectMappingDto> {
    override fun isValid(
        value: ObjectMappingDto?,
        hibernateConstraintValidatorContext: HibernateConstraintValidatorContext,
    ): Boolean {
        if (value == null) {
            return true
        }

        val duplicateKeys = findDuplicateKeys(value)
        if (duplicateKeys.isEmpty()) {
            return true
        }

        hibernateConstraintValidatorContext
            .addMessageParameter(
                UniqueChildrenKeys.DUPLICATE_CHILDREN_KEYS_REF,
                duplicateKeys.joinToString(prefix = "[", postfix = "]") { "'$it'" },
            ).withDynamicPayload(CollectionHelper.toImmutableList(duplicateKeys))

        return false
    }

    fun findDuplicateKeys(value: ObjectMappingDto): List<String> {
        val checkedKeys = mutableSetOf<String>()
        return listOf(
            value.valueMappingPerKey,
            value.valueCollectionMappingPerKey,
            value.objectMappingPerKey,
            value.objectCollectionMappingPerKey,
        ).flatMap { it.keys }
            .filter { !checkedKeys.add(it) }
    }
}
