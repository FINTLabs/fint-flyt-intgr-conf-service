package no.novari.flyt.catalog.integration.api

import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class IntegrationInternalClientControllerTest {
    private lateinit var integrationService: IntegrationService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        integrationService = mock()
        mockMvc = MockMvcBuilders.standaloneSetup(IntegrationInternalClientController(integrationService)).build()
    }

    @Test
    fun `returns integrasjon by sourceApplicationId and sourceApplicationIntegrationId`() {
        whenever(
            integrationService.findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(1L, "abc"),
        ).thenReturn(
            IntegrationDto(
                id = 10L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "abc",
                destination = "fylkesrad",
                state = Integration.State.ACTIVE,
                activeConfigurationId = 20L,
            ),
        )

        mockMvc
            .perform(get("$PATH?sourceApplicationId=1&sourceApplicationIntegrationId=abc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.sourceApplicationIntegrationId").value("abc"))
            .andExpect(jsonPath("$.activeConfigurationId").value(20))
    }

    @Test
    fun `returns 404 for unknown integrasjon`() {
        mockMvc
            .perform(get("$PATH?sourceApplicationId=1&sourceApplicationIntegrationId=unknown"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `returns 400 when sourceApplicationIntegrationId is missing`() {
        mockMvc
            .perform(get("$PATH?sourceApplicationId=1"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns active konfigurasjon id`() {
        whenever(integrationService.findActiveConfigurationIdByIntegrationId(10L)).thenReturn(20L)

        mockMvc
            .perform(get("$PATH/10/aktiv-konfigurasjon-id"))
            .andExpect(status().isOk)
            .andExpect(content().string("20"))
    }

    @Test
    fun `returns 404 when integrasjon has no active konfigurasjon`() {
        // Mockito answers 0 for an unstubbed java.lang.Long, so null must be explicit.
        whenever(integrationService.findActiveConfigurationIdByIntegrationId(10L)).thenReturn(null)

        mockMvc
            .perform(get("$PATH/10/aktiv-konfigurasjon-id"))
            .andExpect(status().isNotFound)
    }

    private companion object {
        const val PATH = "/api/intern-klient/integrasjoner"
    }
}
