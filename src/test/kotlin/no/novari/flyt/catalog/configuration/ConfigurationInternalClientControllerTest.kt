package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.InternalClientResponses
import no.novari.flyt.catalog.configuration.model.configuration.dtos.CollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.FromCollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
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

class ConfigurationInternalClientControllerTest {
    private lateinit var configurationService: ConfigurationService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        configurationService = mock()
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(ConfigurationInternalClientController(configurationService))
                .setMessageConverters(InternalClientResponses.messageConverter())
                .build()
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
    fun `returns the whole mapping tree and nothing else`() {
        whenever(
            configurationService.findById(1L, false),
        ).thenReturn(ConfigurationDto(id = 1L, mapping = mappingTree()))

        mockMvc
            .perform(get("$PATH/1/mapping"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    InternalClientResponses.expected("konfigurasjoner-mapping.json"),
                    JsonCompareMode.STRICT,
                ),
            )
    }

    private fun mappingTree() =
        ObjectMappingDto(
            valueMappingPerKey =
                mutableMapOf(
                    "tittel" to ValueMappingDto(type = ValueMapping.Type.STRING, mappingString = "Sakstittel"),
                ),
            valueCollectionMappingPerKey =
                mutableMapOf(
                    "vedlegg" to
                        CollectionMappingDto(
                            elementMappings =
                                mutableListOf(
                                    ValueMappingDto(type = ValueMapping.Type.FILE, mappingString = "\$if{...}"),
                                ),
                            fromCollectionMappings =
                                mutableListOf(
                                    FromCollectionMappingDto(
                                        instanceCollectionReferencesOrdered = mutableListOf("dokumenter"),
                                        elementMapping =
                                            ValueMappingDto(
                                                type = ValueMapping.Type.DYNAMIC_STRING,
                                                mappingString = "\$if{dokument.navn}",
                                            ),
                                    ),
                                ),
                        ),
                ),
            objectMappingPerKey =
                mutableMapOf(
                    "avsender" to
                        ObjectMappingDto(
                            valueMappingPerKey =
                                mutableMapOf(
                                    "navn" to
                                        ValueMappingDto(
                                            type = ValueMapping.Type.STRING,
                                            mappingString = "Ola Nordmann",
                                        ),
                                ),
                        ),
                ),
        )

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
