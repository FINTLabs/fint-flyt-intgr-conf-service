package no.novari.flyt.catalog.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.novari.flyt.audit.history.EntityHistoryEntryDto
import no.novari.flyt.audit.history.HistoryEntryDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationSnapshot
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidatorFactory
import no.novari.flyt.catalog.configuration.validation.ValidationErrorsFormattingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
class ConfigurationHistoryRoutingWebMvcTest {
    @Mock
    private lateinit var configurationService: ConfigurationService

    @Mock
    private lateinit var configurationValidatorFactory: ConfigurationValidatorFactory

    @Mock
    private lateinit var validationErrorsFormattingService: ValidationErrorsFormattingService

    @Mock
    private lateinit var configurationHistoryService: ConfigurationHistoryService

    @Mock
    private lateinit var authentication: Authentication

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val configurationController =
            ConfigurationController(
                configurationService,
                configurationValidatorFactory,
                validationErrorsFormattingService,
            )
        val historyController = ConfigurationHistoryController(configurationHistoryService)

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(configurationController, historyController)
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .setMessageConverters(MappingJackson2HttpMessageConverter(jacksonObjectMapper()))
                .build()
    }

    @Test
    fun `GET history routes to allHistory and not to getConfiguration`() {
        whenever(configurationHistoryService.findAllHistory(any(), any(), anyOrNull()))
            .thenReturn(PageImpl(emptyList<EntityHistoryEntryDto<ConfigurationSnapshot, Long>>()))

        mockMvc
            .perform(get("/api/intern/konfigurasjoner/history").principal(authentication))
            .andExpect(status().isOk)

        verify(configurationHistoryService).findAllHistory(any(), any(), anyOrNull())
        verify(configurationService, never()).findById(any(), any())
    }

    @Test
    fun `GET id history routes to history for numeric id`() {
        whenever(configurationHistoryService.findHistory(eq(5L), any(), any()))
            .thenReturn(PageImpl(emptyList<HistoryEntryDto<ConfigurationSnapshot>>()))

        mockMvc
            .perform(get("/api/intern/konfigurasjoner/5/history").principal(authentication))
            .andExpect(status().isOk)

        verify(configurationHistoryService).findHistory(eq(5L), any(), any())
    }

    @Test
    fun `GET numeric id routes to getConfiguration and not to history`() {
        whenever(configurationService.findById(5L, false))
            .thenReturn(
                ConfigurationDto
                    .builder()
                    .id(5L)
                    .integrationId(1L)
                    .integrationMetadataId(2L)
                    .build(),
            )

        mockMvc
            .perform(get("/api/intern/konfigurasjoner/5").principal(authentication))
            .andExpect(status().isOk)

        verify(configurationService).findById(5L, false)
        verify(configurationHistoryService, never()).findHistory(any(), any(), any())
    }
}
