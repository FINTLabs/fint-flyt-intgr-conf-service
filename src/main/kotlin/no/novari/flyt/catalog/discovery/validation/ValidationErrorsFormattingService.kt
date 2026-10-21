package no.novari.flyt.catalog.discovery.validation

import jakarta.validation.ConstraintViolation
import org.springframework.stereotype.Service
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

@Service
class ValidationErrorsFormattingService {
    fun <T> format(errors: Set<ConstraintViolation<T>>): String {
        val validationErrors =
            errors
                .map { constraintViolation ->
                    val propertyPath = constraintViolation.propertyPath.toString()
                    val formattedPropertyPath = if (propertyPath.isBlank()) "" else "$propertyPath "
                    "'$formattedPropertyPath${constraintViolation.message}'"
                }.sorted()
                .joinToString(prefix = "[", postfix = "]")

        return "Validation error${if (errors.size > 1) "s" else ""}: $validationErrors"
    }

    fun format(bindingResult: BindingResult): String {
        return format(bindingResult.allErrors)
    }

    private fun format(errors: List<ObjectError>): String {
        val validationErrors =
            errors
                .map { error ->
                    val propertyPath =
                        (error as? FieldError)?.field
                            ?: error.objectName.takeIf { it.isNotBlank() }
                            ?: ""
                    val formattedPropertyPath = if (propertyPath.isBlank()) "" else "$propertyPath "
                    "'$formattedPropertyPath${error.defaultMessage}'"
                }.sorted()
                .joinToString(prefix = "[", postfix = "]")

        return "Validation error${if (errors.size > 1) "s" else ""}: $validationErrors"
    }
}
