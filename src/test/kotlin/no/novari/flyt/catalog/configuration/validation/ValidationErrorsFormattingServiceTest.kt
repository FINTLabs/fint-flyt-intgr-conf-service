package no.novari.flyt.catalog.configuration.validation

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
    fun `formats an empty violation set`() {
        val errors = mutableSetOf<ConstraintViolation<Any>>()
        val result = service.format(errors)
        assertEquals("Validation error: []", result)
    }

    @Test
    fun `formats a single violation with its property path`() {
        val errors = mutableSetOf<ConstraintViolation<Any>>()

        val mockViolation = mock<ConstraintViolation<Any>>()
        whenever(mockViolation.message).thenReturn("Invalid value")
        val mockPath = mock<Path>()
        whenever(mockPath.toString()).thenReturn("field")
        whenever(mockViolation.propertyPath).thenReturn(mockPath)

        errors.add(mockViolation)

        val result = service.format(errors)
        assertEquals("Validation error: ['field Invalid value']", result)
    }

    @Test
    fun `formats multiple violations as a list`() {
        val errors = mutableSetOf<ConstraintViolation<Any>>()

        val mockViolation1 = mock<ConstraintViolation<Any>>()
        whenever(mockViolation1.message).thenReturn("Invalid value")
        val mockPath1 = mock<Path>()
        whenever(mockPath1.toString()).thenReturn("field")
        whenever(mockViolation1.propertyPath).thenReturn(mockPath1)

        val mockViolation2 = mock<ConstraintViolation<Any>>()
        whenever(mockViolation2.message).thenReturn("Invalid format")
        val mockPath2 = mock<Path>()
        whenever(mockPath2.toString()).thenReturn("otherField")
        whenever(mockViolation2.propertyPath).thenReturn(mockPath2)

        errors.add(mockViolation1)
        errors.add(mockViolation2)

        val result = service.format(errors)
        assertEquals("Validation errors: ['field Invalid value', 'otherField Invalid format']", result)
    }

    @Test
    fun `formats a violation whose property path is blank`() {
        val errors = mutableSetOf<ConstraintViolation<Any>>()

        val mockViolation = mock<ConstraintViolation<Any>>()
        whenever(mockViolation.message).thenReturn("Invalid value")
        val mockPath = mock<Path>()
        whenever(mockPath.toString()).thenReturn("")
        whenever(mockViolation.propertyPath).thenReturn(mockPath)

        errors.add(mockViolation)

        val result = service.format(errors)
        assertEquals("Validation error: ['Invalid value']", result)
    }
}
