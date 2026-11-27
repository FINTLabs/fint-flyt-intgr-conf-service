package no.novari.flyt.catalog.integration.api

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.InternalClientResponses
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID

class IntegrationInternalClientControllerTest {
    private lateinit var integrationService: IntegrationService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        integrationService = mock()
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(IntegrationInternalClientController(integrationService))
                .setMessageConverters(InternalClientResponses.messageConverter())
                .build()
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
    fun `returns the whole integrasjon with audit fields`() {
        val first = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val second = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(
            integrationService.findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                1L,
                "kildeapp-integrasjon",
            ),
        ).thenReturn(
            IntegrationDto(
                id = 1L,
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "kildeapp-integrasjon",
                destination = "arkiv",
                state = Integration.State.ACTIVE,
                activeConfigurationId = 100L,
                createdAt = Instant.parse("2026-01-15T09:00:00Z"),
                createdBy = first.toString(),
                createdByActor = Actor.User(first),
                lastModifiedAt = Instant.parse("2026-02-20T13:30:00Z"),
                lastModifiedBy = second.toString(),
                lastModifiedByActor = Actor.User(second),
            ),
        )

        mockMvc
            .perform(get("$PATH?sourceApplicationId=1&sourceApplicationIntegrationId=kildeapp-integrasjon"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    InternalClientResponses.expected("integrasjoner-by-source-application.json"),
                    JsonCompareMode.STRICT,
                ),
            )
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
