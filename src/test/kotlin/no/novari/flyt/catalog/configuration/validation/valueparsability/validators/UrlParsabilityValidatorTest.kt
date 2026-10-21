package no.novari.flyt.catalog.configuration.validation.valueparsability.validators

import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UrlParsabilityValidatorTest {
    private lateinit var urlParsabilityValidator: UrlParsabilityValidator

    @BeforeEach
    fun setUp() {
        urlParsabilityValidator = UrlParsabilityValidator()
    }

    @Test
    fun `reports the value type it validates`() {
        assertEquals(ValueMapping.Type.URL, urlParsabilityValidator.getTypeToValidate())
    }

    @Test
    fun `accepts a value that parses as a URL`() {
        assertTrue(urlParsabilityValidator.isValid("http://www.example.com"))
    }

    @Test
    fun `rejects a value that does not parse as a URL`() {
        assertFalse(urlParsabilityValidator.isValid("httpkk://www.example.com"))
    }

    @Test
    fun `rejects an empty value`() {
        assertFalse(urlParsabilityValidator.isValid(""))
    }

    @Test
    fun `rejects a blank value`() {
        assertFalse(urlParsabilityValidator.isValid(" "))
    }
}
