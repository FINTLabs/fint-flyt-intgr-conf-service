package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BooleanParsabilityValidatorTest {
    private lateinit var booleanParsabilityValidator: BooleanParsabilityValidator

    @BeforeEach
    fun setUp() {
        booleanParsabilityValidator = BooleanParsabilityValidator()
    }

    @Test
    fun `reports the value type it validates`() {
        assertEquals(ValueMapping.Type.BOOLEAN, booleanParsabilityValidator.getTypeToValidate())
    }

    @Test
    fun `accepts the value true`() {
        assertTrue(booleanParsabilityValidator.isValid("true"))
    }

    @Test
    fun `accepts the value false`() {
        assertTrue(booleanParsabilityValidator.isValid("false"))
    }

    @Test
    fun `rejects an empty value`() {
        assertFalse(booleanParsabilityValidator.isValid(""))
    }

    @Test
    fun `rejects a blank value`() {
        assertFalse(booleanParsabilityValidator.isValid(" "))
    }

    @Test
    fun `rejects a value that only looks like a boolean`() {
        assertFalse(booleanParsabilityValidator.isValid("truest"))
    }
}
