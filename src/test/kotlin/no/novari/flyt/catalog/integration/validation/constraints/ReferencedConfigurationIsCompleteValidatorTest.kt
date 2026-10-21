package no.novari.flyt.catalog.integration.validation.constraints

import no.novari.flyt.catalog.integration.api.dto.ConfigurationDto
import no.novari.flyt.catalog.integration.validation.IntegrationValidationContext
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReferencedConfigurationIsCompleteValidatorTest {
    private lateinit var validator: ReferencedConfigurationIsCompleteValidator
    private lateinit var validationContext: IntegrationValidationContext

    @BeforeEach
    fun setUp() {
        validator = ReferencedConfigurationIsCompleteValidator()
        validationContext = mock()
    }

    @Test
    fun `returns true for null value`() {
        val result = validator.isValid(null, createMockContext())

        assertTrue(result)
    }

    @Test
    fun `returns true when configuration is completed`() {
        whenever(validationContext.configuration).thenReturn(ConfigurationDto(completed = true))

        val result = validator.isValid(1L, createMockContext())

        assertTrue(result)
    }

    @Test
    fun `returns false when configuration is not completed`() {
        whenever(validationContext.configuration).thenReturn(ConfigurationDto(completed = false))

        val result = validator.isValid(1L, createMockContext())

        assertFalse(result)
    }

    private fun createMockContext(): HibernateConstraintValidatorContext {
        val mockContext = mock<HibernateConstraintValidatorContext>()
        whenever(mockContext.getConstraintValidatorPayload(IntegrationValidationContext::class.java))
            .thenReturn(validationContext)
        return mockContext
    }
}
