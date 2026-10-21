package no.novari.flyt.catalog.integration.validation.constraints

import no.novari.flyt.catalog.integration.api.dto.ConfigurationDto
import no.novari.flyt.catalog.integration.validation.IntegrationValidationContext
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReferencedConfigurationExistsValidatorTest {
    @Test
    fun `validates that the referenced configuration exists`() {
        val context = mock<HibernateConstraintValidatorContext>()
        val validationContext = mock<IntegrationValidationContext>()
        val validator = ReferencedConfigurationExistsValidator()

        whenever(context.getConstraintValidatorPayload(IntegrationValidationContext::class.java))
            .thenReturn(validationContext)

        assertTrue(validator.isValid(null, context))
        assertFalse(validator.isValid(1L, context))

        whenever(validationContext.configuration)
            .thenReturn(
                ConfigurationDto(
                    id = 1L,
                    integrationId = 2L,
                    integrationMetadataId = 3L,
                    completed = true,
                    comment = "Test Comment",
                    version = 1,
                ),
            )

        assertTrue(validator.isValid(1L, context))
    }
}
