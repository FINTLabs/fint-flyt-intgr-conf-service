package no.novari.flyt.catalog.valueconverting.api

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.InternalClientResponses
import no.novari.flyt.catalog.setInheritedField
import no.novari.flyt.catalog.valueconverting.domain.ValueConversion
import no.novari.flyt.catalog.valueconverting.infrastructure.persistence.ValueConversionRepository
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
import java.util.Optional
import java.util.UUID

class ValueConversionInternalClientControllerTest {
    private lateinit var valueConversionRepository: ValueConversionRepository
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        valueConversionRepository = mock()
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(ValueConversionInternalClientController(valueConversionRepository))
                .setMessageConverters(InternalClientResponses.messageConverter())
                .build()
    }

    @Test
    fun `returns value conversion with the same shape as the Kafka reply`() {
        whenever(valueConversionRepository.findById(40L)).thenReturn(
            Optional.of(
                ValueConversion(
                    id = 40L,
                    displayName = "Arkivressurs",
                    fromApplicationId = 1L,
                    fromTypeId = "fra",
                    toApplicationId = "fint",
                    toTypeId = "til",
                    convertingMap = mutableMapOf("a" to "b"),
                ),
            ),
        )

        mockMvc
            .perform(get("$PATH/40"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Arkivressurs"))
            .andExpect(jsonPath("$.convertingMap.a").value("b"))
            .andExpect(jsonPath("$.id").doesNotExist())
    }

    @Test
    fun `returns the whole value conversion with audit fields and without id`() {
        val stored =
            ValueConversion(
                id = 1L,
                displayName = "Display name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = mutableMapOf("A" to "B"),
            ).apply {
                setInheritedField("createdAt", Instant.parse("2026-01-15T09:00:00Z"))
                setInheritedField("createdBy", Actor.User(UUID.fromString("11111111-1111-1111-1111-111111111111")))
                setInheritedField("lastModifiedAt", Instant.parse("2026-02-20T13:30:00Z"))
                setInheritedField("lastModifiedBy", Actor.User(UUID.fromString("22222222-2222-2222-2222-222222222222")))
            }
        whenever(valueConversionRepository.findById(1L)).thenReturn(Optional.of(stored))

        mockMvc
            .perform(get("$PATH/1"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    InternalClientResponses.expected("value-convertings-by-id.json"),
                    JsonCompareMode.STRICT,
                ),
            )
    }

    @Test
    fun `returns 404 for unknown value conversion`() {
        whenever(valueConversionRepository.findById(40L)).thenReturn(Optional.empty())

        mockMvc
            .perform(get("$PATH/40"))
            .andExpect(status().isNotFound)
    }

    private companion object {
        const val PATH = "/api/intern-klient/value-convertings"
    }
}
