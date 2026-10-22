package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValueConvertingParsabilityValidatorTest {
    private lateinit var validator: ValueConvertingParsabilityValidator

    @BeforeEach
    fun setUp() {
        validator = ValueConvertingParsabilityValidator()
    }

    @Test
    fun `reports the value type it validates`() {
        assertEquals(ValueMapping.Type.VALUE_CONVERTING, validator.getTypeToValidate())
    }

    @Test
    fun `accepts a value converting reference combined with an instance field reference`() {
        assertTrue(validator.isValid("\$vc{0}\$if{fornavn}"))
    }

    @Test
    fun `accepts a value converting reference combined with an instance collection field reference`() {
        assertTrue(validator.isValid("\$vc{0}\$icf{0}{fornavn}"))
    }

    @Test
    fun `rejects an empty string`() {
        assertFalse(validator.isValid(""))
    }

    @Test
    fun `rejects a blank string`() {
        assertFalse(validator.isValid(" "))
    }

    @Test
    fun `rejects a string without dynamic values`() {
        assertFalse(validator.isValid("Søknad VGS"))
    }

    @Test
    fun `rejects a value converting reference with wrong syntax`() {
        assertFalse(validator.isValid("asd\$if{fornavn}"))
    }

    @Test
    fun `rejects a value converting reference whose id is not numeric`() {
        assertFalse(validator.isValid("\$vc(asd)\$if{fornavn}"))
    }

    @Test
    fun `rejects a string where the value converting reference comes last`() {
        assertFalse(validator.isValid("\$if{fornavn}\$vc(asd)"))
    }

    @Test
    fun `rejects a string without a value converting reference`() {
        assertFalse(validator.isValid("\$if{fornavn}"))
    }

    @Test
    fun `rejects a string without an instance field reference`() {
        assertFalse(validator.isValid("\$if{fornavn}"))
    }

    @Test
    fun `rejects an otherwise valid reference followed by additional text`() {
        assertFalse(validator.isValid("\$vc(0)a\$if{fornavn}"))
    }
}
