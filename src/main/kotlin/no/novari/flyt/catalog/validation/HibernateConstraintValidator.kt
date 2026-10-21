package no.novari.flyt.catalog.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext

interface HibernateConstraintValidator<A : Annotation, T> : ConstraintValidator<A, T> {
    override fun isValid(
        value: T?,
        constraintValidatorContext: ConstraintValidatorContext,
    ): Boolean {
        if (constraintValidatorContext is HibernateConstraintValidatorContext) {
            val hibernateConstraintValidatorContext =
                constraintValidatorContext.unwrap(HibernateConstraintValidatorContext::class.java)
            return isValid(value, hibernateConstraintValidatorContext)
        }

        throw IllegalStateException("Validator is not HibernateConstraintValidatorContext")
    }

    fun isValid(
        value: T?,
        hibernateConstraintValidatorContext: HibernateConstraintValidatorContext,
    ): Boolean
}
