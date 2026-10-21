package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.valueparsability.ValueParsabilityValidator
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ValueParsableAsTypeValidatorTest {
    private lateinit var valueParsabilityValidator1: ValueParsabilityValidator
    private lateinit var valueParsabilityValidator2: ValueParsabilityValidator
    private lateinit var valueParsableAsTypeValidator: ValueParsableAsTypeValidator
    private lateinit var hibernateContext: HibernateConstraintValidatorContext

    @BeforeEach
    fun setUp() {
        valueParsabilityValidator1 = mock()
        valueParsabilityValidator2 = mock()
        valueParsableAsTypeValidator =
            ValueParsableAsTypeValidator(listOf(valueParsabilityValidator1, valueParsabilityValidator2))
        hibernateContext = mock()
        whenever(hibernateContext.addMessageParameter(any(), any())).thenReturn(hibernateContext)
    }

    @Test
    fun `accepts when every field parsability validator accepts`() {
        whenever(valueParsabilityValidator1.isValid(any<ValueMappingDto>())).thenReturn(true)
        whenever(valueParsabilityValidator2.isValid(any<ValueMappingDto>())).thenReturn(true)

        val valid =
            valueParsableAsTypeValidator.isValid(
                ValueMappingDto.builder().type(ValueMapping.Type.STRING).build(),
                hibernateContext,
            )

        assertTrue(valid)
    }

    @Test
    fun `rejects when a single field parsability validator rejects`() {
        whenever(valueParsabilityValidator1.isValid(any<ValueMappingDto>())).thenReturn(true)
        whenever(valueParsabilityValidator2.isValid(any<ValueMappingDto>())).thenReturn(false)

        val valid =
            valueParsableAsTypeValidator.isValid(
                ValueMappingDto.builder().type(ValueMapping.Type.STRING).build(),
                hibernateContext,
            )

        assertFalse(valid)
    }
}
