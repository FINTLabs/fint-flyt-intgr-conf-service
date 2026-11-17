package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ConfigurationInternalClientControllerTest {
    private lateinit var configurationService: ConfigurationService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        configurationService = mock()
        mockMvc = MockMvcBuilders.standaloneSetup(ConfigurationInternalClientController(configurationService)).build()
    }

    @Test
    fun `returns mapping for konfigurasjon`() {
        val mapping =
            ObjectMappingDto(
                valueMappingPerKey =
                    mutableMapOf("tittel" to ValueMappingDto(type = ValueMapping.Type.STRING, mappingString = "x")),
            )
        whenever(configurationService.findById(30L, false))
            .thenReturn(ConfigurationDto(id = 30L, mapping = mapping))

        mockMvc
            .perform(get("$PATH/30/mapping"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valueMappingPerKey.tittel.mappingString").value("x"))
    }

    @Test
    fun `returns 404 for unknown konfigurasjon`() {
        mockMvc
            .perform(get("$PATH/30/mapping"))
            .andExpect(status().isNotFound)
    }

    private companion object {
        const val PATH = "/api/intern-klient/konfigurasjoner"
    }
}
