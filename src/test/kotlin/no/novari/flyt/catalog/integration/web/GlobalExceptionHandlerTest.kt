package no.novari.flyt.catalog.integration.web

import no.novari.flyt.catalog.validation.ValidationErrorsFormattingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler(ValidationErrorsFormattingService())

    @Test
    fun `returns generic problem detail for forbidden without body exception`() {
        val response = handler.handleResponseStatusException(ForbiddenWithoutBodyException())

        assertEquals(403, response.status)
        assertEquals("Forbidden", response.title)
        assertNull(response.detail)
    }

    @Test
    fun `renders safe specific message for response status exceptions`() {
        val response =
            handler.handleResponseStatusException(
                UnprocessableEntityException("Validation errors: ['activeConfigurationId invalid']"),
            )

        assertEquals(422, response.status)
        assertEquals("Unprocessable Entity", response.title)
        assertEquals("Validation errors: ['activeConfigurationId invalid']", response.detail)
    }

    @Test
    fun `handles response status exception thrown by dependencies`() {
        val response =
            handler.handleResponseStatusException(
                ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission"),
            )

        assertEquals(403, response.status)
        assertEquals("Forbidden", response.title)
        assertEquals("You do not have permission", response.detail)
    }

    @Test
    fun `hides unexpected exception details`() {
        val response = handler.handleUnexpectedException(RuntimeException("boom"))

        assertEquals(500, response.status)
        assertEquals("Internal Server Error", response.title)
        assertEquals("An unexpected error occurred", response.detail)
    }
}
