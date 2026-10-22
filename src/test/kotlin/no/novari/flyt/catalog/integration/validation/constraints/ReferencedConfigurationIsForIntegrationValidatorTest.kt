package no.novari.flyt.catalog.integration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.integration.validation.IntegrationValidationContext
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReferencedConfigurationIsForIntegrationValidatorTest {
    private lateinit var validator: ReferencedConfigurationIsForIntegrationValidator
    private lateinit var validationContext: IntegrationValidationContext

    @BeforeEach
    fun setUp() {
        validator = ReferencedConfigurationIsForIntegrationValidator()
        validationContext = mock()
    }

    @Test
    fun `returns true for null value`() {
        val result = validator.isValid(null, createMockContext())

        assertTrue(result)
    }

    @Test
    fun `returns true when configuration belongs to integration`() {
        whenever(validationContext.integrationId).thenReturn(12345L)
        whenever(validationContext.configuration).thenReturn(ConfigurationDto(integrationId = 12345L))

        val result = validator.isValid(1L, createMockContext())

        assertTrue(result)
    }

    @Test
    fun `returns false when configuration belongs to another integration`() {
        whenever(validationContext.integrationId).thenReturn(12345L)
        whenever(validationContext.configuration).thenReturn(ConfigurationDto(integrationId = 67890L))

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
