package no.novari.flyt.catalog.integration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.catalog.integration.validation.IntegrationValidationContext
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ActiveConfigurationIsDefinedIfStateIsActiveValidatorTest {
    private lateinit var validator: ActiveConfigurationIsDefinedIfStateIsActiveValidator
    private lateinit var context: HibernateConstraintValidatorContext
    private lateinit var validationContext: IntegrationValidationContext

    @BeforeEach
    fun setUp() {
        validator = ActiveConfigurationIsDefinedIfStateIsActiveValidator()
        context = mock()
        validationContext = mock()
    }

    @Test
    fun `returns true when state is not active`() {
        whenever(context.getConstraintValidatorPayload(IntegrationValidationContext::class.java))
            .thenReturn(validationContext)

        val result = validator.isValid(Integration.State.DEACTIVATED, context)

        assertTrue(result)
    }

    @Test
    fun `returns true when state is active and configuration is present`() {
        whenever(context.getConstraintValidatorPayload(IntegrationValidationContext::class.java))
            .thenReturn(validationContext)
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

        val result = validator.isValid(Integration.State.ACTIVE, context)

        assertTrue(result)
    }

    @Test
    fun `returns false when state is active and configuration is missing`() {
        whenever(context.getConstraintValidatorPayload(IntegrationValidationContext::class.java))
            .thenReturn(validationContext)
        whenever(validationContext.configuration).thenReturn(null)

        val result = validator.isValid(Integration.State.ACTIVE, context)

        assertFalse(result)
    }
}
