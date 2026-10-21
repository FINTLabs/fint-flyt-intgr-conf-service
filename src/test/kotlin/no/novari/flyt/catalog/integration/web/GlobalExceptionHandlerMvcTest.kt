package no.novari.flyt.catalog.integration.web

import no.novari.flyt.catalog.integration.api.IntegrationController
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.application.IntegrationUpdateValidationService
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Feilhåndtereren er avgrenset til [IntegrationController], så testen må gå gjennom den ekte
 * kontrolleren. En probe-kontroller ville ikke lenger nådd advicen.
 */
class GlobalExceptionHandlerMvcTest {
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                IntegrationController(
                    mock<IntegrationService>(),
                    mock<IntegrationUpdateValidationService>(),
                    mock<UserAuthorizationService>(),
                ),
            ).setControllerAdvice(GlobalExceptionHandler(mock()))
            .build()

    @Test
    fun `malformed request body returns 400 problem detail`() {
        mockMvc
            .perform(
                post("/api/intern/integrasjoner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ not valid json"),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `path variable type mismatch returns 400 problem detail`() {
        mockMvc
            .perform(get("/api/intern/integrasjoner/not-a-number"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
    }
}
