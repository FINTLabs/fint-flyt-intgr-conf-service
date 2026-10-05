package no.novari.flyt.catalog.valueconverting.api

import no.novari.flyt.catalog.valueconverting.application.ValueConversionService
import no.novari.flyt.catalog.valueconverting.domain.ValueConversionHistoryService
import no.novari.flyt.catalog.valueconverting.infrastructure.persistence.ValueConversionRepository
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ValueConversionExceptionHandlerMvcTest {
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                ValueConversionController(mock<ValueConversionService>(), mock<UserAuthorizationService>()),
                ValueConversionHistoryController(
                    mock<ValueConversionRepository>(),
                    mock<UserAuthorizationService>(),
                    mock<ValueConversionHistoryService>(),
                ),
            ).setControllerAdvice(ValueConversionExceptionHandler())
            .build()

    @Test
    fun `path variable type mismatch returns 400 problem detail`() {
        mockMvc
            .perform(get("/api/intern/value-convertings/not-a-number"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Invalid value for request parameter 'valueConversionId'"))
    }

    @Test
    fun `path variable type mismatch on the history surface returns 400 problem detail`() {
        mockMvc
            .perform(get("/api/intern/value-convertings/not-a-number/history"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Invalid value for request parameter 'id'"))
    }
}
