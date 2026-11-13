package no.novari.flyt.catalog.valueconverting.api

import no.novari.flyt.catalog.valueconverting.domain.ValueConversion
import no.novari.flyt.catalog.valueconverting.infrastructure.persistence.ValueConversionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.Optional

class ValueConversionInternalClientControllerTest {
    private lateinit var valueConversionRepository: ValueConversionRepository
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        valueConversionRepository = mock()
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(ValueConversionInternalClientController(valueConversionRepository))
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
