package no.novari.flyt.catalog.configuration

import jakarta.servlet.http.HttpServletRequest
import no.novari.flyt.catalog.configuration.validation.CouldNotFindMetadataException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException

class ApiExceptionHandlerTest {
    private val exceptionHandler = ApiExceptionHandler()

    @Test
    fun `keeps the validation message verbatim in the error response`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/ofk-no/api/intern/konfigurasjoner/210")

        val response =
            exceptionHandler.handleResponseStatusException(
                ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Validation error: ['mapping.valueMappingPerKey contains references to instance values that are not defined in the metadata: ['skjema.Logic.Kommune_til_tittel1']']",
                ),
                request,
            )

        assertEquals(422, response.statusCode.value())
        assertEquals("Unprocessable Entity", response.body?.error)
        assertEquals(
            "Validation error: ['mapping.valueMappingPerKey contains references to instance values that are not defined in the metadata: ['skjema.Logic.Kommune_til_tittel1']']",
            response.body?.message,
        )
        assertEquals("/ofk-no/api/intern/konfigurasjoner/210", response.body?.path)
        assertTrue(response.body?.timestamp != null)
    }

    @Test
    fun `keeps the message verbatim when a metadata lookup fails`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/ofk-no/api/intern/konfigurasjoner/210")

        val response =
            exceptionHandler.handleConfigurationValidationContextException(
                CouldNotFindMetadataException(98L),
                request,
            )

        assertEquals(422, response.statusCode.value())
        assertEquals("Unprocessable Entity", response.body?.error)
        assertEquals("Could not find metadata with id=98", response.body?.message)
        assertEquals("/ofk-no/api/intern/konfigurasjoner/210", response.body?.path)
        assertTrue(response.body?.timestamp != null)
    }

    @Test
    fun `maps an illegal argument to bad request and keeps the message`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/ofk-no/api/intern/konfigurasjoner")

        val response =
            exceptionHandler.handleIllegalArgumentException(
                IllegalArgumentException("Required value was null."),
                request,
            )

        assertEquals(400, response.statusCode.value())
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Required value was null.", response.body?.message)
        assertEquals("/ofk-no/api/intern/konfigurasjoner", response.body?.path)
        assertTrue(response.body?.timestamp != null)
    }

    @Test
    fun `maps an unhandled throwable to internal server error without leaking the message`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/ofk-no/api/intern/konfigurasjoner")

        val response =
            exceptionHandler.handleThrowable(
                IllegalStateException("connection to jdbc:postgresql://db:5432 refused for user fintlabs"),
                request,
            )

        assertEquals(500, response.statusCode.value())
        assertEquals("Internal Server Error", response.body?.error)
        assertEquals("Internal server error", response.body?.message)
        assertEquals("/ofk-no/api/intern/konfigurasjoner", response.body?.path)
        assertTrue(response.body?.timestamp != null)
    }

    @Test
    fun `keeps the framework status for spring mvc exceptions instead of falling back to the catch all`() {
        val webRequest =
            ServletWebRequest(
                MockHttpServletRequest("GET", "/ofk-no/api/intern/ukjent"),
                MockHttpServletResponse(),
            )

        val response =
            exceptionHandler.handleException(
                NoResourceFoundException(HttpMethod.GET, "/ofk-no/api/intern/ukjent"),
                webRequest,
            )

        assertNotNull(response)
        assertEquals(404, response!!.statusCode.value())

        val body = response.body as ApiErrorResponse
        assertEquals("Not Found", body.error)
        assertEquals("/ofk-no/api/intern/ukjent", body.path)
    }
}
