package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class AtLeastOneChildValidatorTest {
    private lateinit var atLeastOneChildValidator: AtLeastOneChildValidator

    @BeforeEach
    fun setUp() {
        atLeastOneChildValidator = AtLeastOneChildValidator()
    }

    @Test
    fun `accepts an object mapping that has children`() {
        val objectMappingDto = ObjectMappingDto.builder().build()
        objectMappingDto.valueMappingPerKey["valueMappingKey"] = mock<ValueMappingDto>()

        val valid =
            atLeastOneChildValidator.isValid(
                objectMappingDto,
                mock<HibernateConstraintValidatorContext>(),
            )

        assertTrue(valid)
    }

    @Test
    fun `rejects an object mapping without children`() {
        val valid =
            atLeastOneChildValidator.isValid(
                ObjectMappingDto.builder().build(),
                mock<HibernateConstraintValidatorContext>(),
            )

        assertFalse(valid)
    }
}
