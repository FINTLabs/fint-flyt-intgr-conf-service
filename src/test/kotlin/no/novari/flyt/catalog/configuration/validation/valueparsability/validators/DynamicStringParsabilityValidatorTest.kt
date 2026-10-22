package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DynamicStringParsabilityValidatorTest {
    private lateinit var dynamicStringParsabilityValidator: DynamicStringParsabilityValidator

    @BeforeEach
    fun setUp() {
        dynamicStringParsabilityValidator = DynamicStringParsabilityValidator()
    }

    @Test
    fun `reports the value type it validates`() {
        assertEquals(ValueMapping.Type.DYNAMIC_STRING, dynamicStringParsabilityValidator.getTypeToValidate())
    }

    @Test
    fun `accepts an empty string`() {
        assertTrue(dynamicStringParsabilityValidator.isValid(""))
    }

    @Test
    fun `accepts a blank string`() {
        assertTrue(dynamicStringParsabilityValidator.isValid(" "))
    }

    @Test
    fun `accepts a string without dynamic values`() {
        assertTrue(dynamicStringParsabilityValidator.isValid("Søknad VGS"))
    }

    @Test
    fun `accepts a string whose dynamic values use the correct syntax`() {
        assertTrue(
            dynamicStringParsabilityValidator.isValid(
                "Søknad VGS \$if{fornavn}\$if{etter-navn} \$if{person nr1 fødselsdato} for dato \$if{dato} ettellerannet",
            ),
        )
    }

    @Test
    fun `accepts special characters that are not part of an instance field reference`() {
        assertTrue(dynamicStringParsabilityValidator.isValid("Søknad VGS \\$ { } \$if"))
    }

    @Test
    fun `rejects an incomplete instance field reference`() {
        assertFalse(dynamicStringParsabilityValidator.isValid("Søknad VGS \$if{fornavn"))
    }

    @Test
    fun `rejects an empty instance field reference`() {
        assertFalse(dynamicStringParsabilityValidator.isValid("Søknad VGS \$if{}"))
    }

    @Test
    fun `rejects an instance field reference nested inside another`() {
        assertFalse(dynamicStringParsabilityValidator.isValid("Søknad VGS \$if{abc\$if{123}}"))
    }

    @Test
    fun `rejects a reference starter nested inside an instance field reference`() {
        assertFalse(dynamicStringParsabilityValidator.isValid("Søknad VGS \$if{abc\$if{}"))
    }
}
