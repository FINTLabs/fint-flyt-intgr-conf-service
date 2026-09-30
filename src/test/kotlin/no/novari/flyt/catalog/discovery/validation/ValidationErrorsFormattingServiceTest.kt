package no.novari.flyt.catalog.discovery.validation

import jakarta.validation.ConstraintViolation
import jakarta.validation.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ValidationErrorsFormattingServiceTest {
    private lateinit var service: ValidationErrorsFormattingService

    @BeforeEach
    fun setUp() {
        service = ValidationErrorsFormattingService()
    }

    @Test
    fun `formats a single validation error`() {
        val errors = hashSetOf(createConstraintViolation("field1", "error message 1"))

        val result = service.format(errors)

        assertEquals("Validation error: ['field1 error message 1']", result)
    }

    @Test
    fun `formats and sorts multiple validation errors`() {
        val errors =
            hashSetOf(
                createConstraintViolation("field2", "error message 2"),
                createConstraintViolation("field1", "error message 1"),
            )

        val result = service.format(errors)

        assertEquals("Validation errors: ['field1 error message 1', 'field2 error message 2']", result)
    }

    @Test
    fun `formats a validation error with a blank field`() {
        val errors = hashSetOf(createConstraintViolation("", "error message 1"))

        val result = service.format(errors)

        assertEquals("Validation error: ['error message 1']", result)
    }

    @Suppress("UNCHECKED_CAST")
    private fun createConstraintViolation(
        field: String,
        message: String,
    ): ConstraintViolation<Any> {
        val violation = mock<ConstraintViolation<Any>>()
        val path = mock<Path>()

        whenever(violation.propertyPath).thenReturn(path)
        whenever(path.toString()).thenReturn(field)
        whenever(violation.message).thenReturn(message)

        return violation
    }
}
