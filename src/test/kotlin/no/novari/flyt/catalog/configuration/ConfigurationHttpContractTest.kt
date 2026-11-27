package no.novari.flyt.catalog.configuration

import jakarta.validation.Validation
import jakarta.validation.Validator
import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.audit.history.AuditPropertyFilter
import no.novari.flyt.audit.history.EntityHistoryEntryDto
import no.novari.flyt.audit.history.HistoryEntryDto
import no.novari.flyt.audit.history.HistoryEventType
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationSnapshot
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidationContext
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidatorFactory
import no.novari.flyt.catalog.configuration.validation.CouldNotFindIntegrationException
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.FixtureObjectMapper
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixture
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixtureRunner
import no.novari.flyt.catalog.integration.persistence.IntegrationRepository
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.catalog.security.AuthorizationService
import no.novari.flyt.catalog.validation.ValidationErrorsFormattingService
import no.novari.flyt.security.FlytAccess
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.validator.HibernateValidator
import org.hibernate.validator.HibernateValidatorFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.Optional
import java.util.UUID

/**
 * Fastholder HTTP-kontrakten for configuration-domenet slik den er i dag, mot fixturene i
 * `src/test/resources/catalog-contract-fixtures`.
 *
 * Tilgangen er ekte: `ConfigurationAccess` og `AuthorizationService` kjører som i drift, og bare `FlytAccess` og
 * repositoryene er stubbet. Brukeren har kildeapplikasjon 1 (integrasjon 10), ikke 2 (integrasjon 20) (FFS-2431).
 */
class ConfigurationHttpContractTest {
    private lateinit var configurationService: ConfigurationService
    private lateinit var configurationValidatorFactory: ConfigurationValidatorFactory
    private lateinit var configurationHistoryService: ConfigurationHistoryService
    private lateinit var authentication: Authentication
    private lateinit var flytAccess: FlytAccess
    private lateinit var integrationRepository: IntegrationRepository
    private lateinit var configurationRepository: ConfigurationRepository
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        configurationService = mock()
        configurationValidatorFactory = mock()
        configurationHistoryService = mock()
        authentication = mock()
        flytAccess = mock()
        integrationRepository = mock()
        configurationRepository = mock()
        stubAccess()
        val configurationAccess = ConfigurationAccess(integrationRepository, AuthorizationService(flytAccess))

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    ConfigurationController(
                        configurationService,
                        configurationValidatorFactory,
                        ValidationErrorsFormattingService(),
                        configurationAccess,
                    ),
                    ConfigurationHistoryController(
                        configurationRepository,
                        configurationAccess,
                        configurationHistoryService,
                    ),
                ).setControllerAdvice(ApiExceptionHandler())
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .setMessageConverters(JacksonJsonHttpMessageConverter(OBJECT_MAPPER.rebuild()))
                .build()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("httpContractFixtures")
    fun `HTTP-kontrakten er uendret`(fixture: HttpContractFixture) {
        stubServiceLayerFor(fixture)

        HttpContractFixtureRunner(
            mockMvc = mockMvc,
            objectMapper = OBJECT_MAPPER,
            customizeRequest = { it.principal(authentication) },
        ).verify(fixture)

        verifyDeserializedRequestFor(fixture)
        verifyNothingChangedFor(fixture)
    }

    /** A 403 for missing access must come before anything is saved or deleted. */
    private fun verifyNothingChangedFor(fixture: HttpContractFixture) {
        if (fixture.id in
            setOf(
                "configuration/post/forbidden",
                "configuration/patch/forbidden-no-access",
                "configuration/delete/forbidden-no-access",
            )
        ) {
            verify(configurationService, never()).save(any())
            verify(configurationService, never()).updateById(any(), any())
            verify(configurationService, never()).deleteById(any())
        }
    }

    private fun stubAccess() {
        whenever(flytAccess.sourceApplicationIds()).thenReturn(setOf(1L))
        whenever(flytAccess.hasSourceApplication(any())).thenAnswer { it.getArgument<Long>(0) == 1L }
        whenever(
            integrationRepository.findById(10L),
        ).thenReturn(Optional.of(integration(10L, sourceApplicationId = 1L)))
        whenever(
            integrationRepository.findById(20L),
        ).thenReturn(Optional.of(integration(20L, sourceApplicationId = 2L)))
        whenever(integrationRepository.findDistinctSourceApplicationIds()).thenReturn(setOf(1L, 2L))
        whenever(integrationRepository.findIdsBySourceApplicationIdIn(setOf(1L))).thenReturn(setOf(10L))
        whenever(
            configurationRepository.findById(2L),
        ).thenReturn(Optional.of(Configuration(id = 2L, integrationId = 20L)))
    }

    private fun integration(
        id: Long,
        sourceApplicationId: Long,
    ) = Integration(
        id = id,
        sourceApplicationId = sourceApplicationId,
        sourceApplicationIntegrationId = "integrasjon-$id",
        destination = "arkiv",
        state = Integration.State.DEACTIVATED,
    )

    /**
     * Responsen kommer fra stubben, ikke fra det som ble lest inn, så den dekker ikke
     * request-kontrakten. Mapping-treet er den delen som betyr mest her: det er hele nyttelasten
     * frontend sender, og et tapt nivå ville blitt stille ignorert av Jackson.
     */
    private fun verifyDeserializedRequestFor(fixture: HttpContractFixture) {
        if (fixture.id != "configuration/post/ok") {
            return
        }

        val posted = argumentCaptor<ConfigurationDto>()
        verify(configurationService).save(posted.capture())

        assertThat(posted.firstValue).usingRecursiveComparison().isEqualTo(
            ConfigurationDto(
                integrationId = 10L,
                integrationMetadataId = 100L,
                completed = false,
                comment = "Kommentar",
                mapping = mappingTree(),
            ),
        )
    }

    private fun stubServiceLayerFor(fixture: HttpContractFixture) {
        when (fixture.id) {
            "configuration/list/ok" -> {
                stubList(configuration())
            }

            "configuration/list/ok-exclude-mapping" -> {
                stubList(configuration(mapping = null))
            }

            "configuration/list/ok-filtered" -> {
                stubList(
                    configuration(
                        id = 2L,
                        completed = true,
                        comment = "Ferdigstilt",
                        version = 3,
                        mapping = null,
                    ),
                )
            }

            "configuration/list/ok-authorized-only" -> {
                // Only a filter limited to the user's integration gets the row, so the filter is part of the check.
                val row =
                    configuration(
                        id = 1L,
                        comment = "Tilhører kildeapplikasjon brukeren har tilgang til",
                        mapping = null,
                    )
                whenever(configurationService.findAll(argThat { integrationIds == setOf(10L) }, any(), any()))
                    .thenReturn(PageImpl(listOf(row), PageRequest.of(0, 20), 1))
            }

            "configuration/list/forbidden-integration",
            "configuration/post/forbidden",
            -> {}

            "configuration/get-by-id/ok" -> {
                whenever(configurationService.findById(1L, false)).thenReturn(configuration())
            }

            "configuration/get-by-id/forbidden",
            "configuration/patch/forbidden-no-access",
            "configuration/delete/forbidden-no-access",
            -> {
                whenever(configurationService.findById(eq(2L), any())).thenReturn(
                    configuration(
                        id = 2L,
                        integrationId = 20L,
                        integrationMetadataId = 200L,
                        comment = "Tilhører kildeapplikasjon brukeren IKKE har tilgang til",
                        mapping = null,
                    ),
                )
            }

            "configuration/get-by-id/not-found" -> {
                whenever(configurationService.findById(eq(123L), any())).thenReturn(null)
            }

            "configuration/post/ok" -> {
                stubRealValidator()
                whenever(configurationService.save(any())).thenReturn(configuration())
            }

            "configuration/post/unprocessable-single-validation-error",
            "configuration/post/unprocessable-multiple-validation-errors",
            -> {
                stubRealValidator()
            }

            "configuration/post/unprocessable-integration-not-found" -> {
                whenever(configurationValidatorFactory.getValidator(eq(999L), any()))
                    .thenThrow(CouldNotFindIntegrationException(999L))
            }

            "configuration/patch/not-found",
            "configuration/delete/not-found",
            -> {
                whenever(configurationService.findById(eq(123L), any())).thenReturn(null)
            }

            "configuration/patch/forbidden-completed",
            "configuration/delete/forbidden-completed",
            -> {
                whenever(configurationService.findById(eq(1L), any())).thenReturn(configuration(completed = true))
            }

            "configuration/delete/no-content" -> {
                whenever(configurationService.findById(1L, true)).thenReturn(configuration())
            }

            "configuration/all-history/ok-authorized-only" -> {
                // Only the filter on the user's integration gets the history, so the filter is part of the check.
                whenever(
                    configurationHistoryService.findAllHistory(
                        any(),
                        any(),
                        eq(AuditPropertyFilter(property = "integrationId", allowedValues = setOf(10L))),
                    ),
                ).thenReturn(
                    PageImpl(
                        listOf(
                            EntityHistoryEntryDto(
                                entityId = 1L,
                                timestamp = LAST_MODIFIED_AT,
                                type = HistoryEventType.UPDATED,
                                actor = Actor.User(SECOND_ACTOR_OID),
                                actorDisplay = "Ola Nordmann",
                                snapshot =
                                    snapshot(
                                        id = 1L,
                                        version = 2,
                                        comment = "Tilhører kildeapplikasjon brukeren har tilgang til",
                                    ),
                            ),
                        ),
                        PageRequest.of(0, 20),
                        1,
                    ),
                )
            }

            "configuration/history-by-id/forbidden" -> {}

            else -> {
                error(
                    "Fixturen '${fixture.id}' har ikke oppsett i denne testen. " +
                        "Legg det til her, ellers er kontrakten udekket i denne tjenesten.",
                )
            }
        }
    }

    private fun stubList(vararg content: ConfigurationDto) {
        whenever(configurationService.findAll(any(), any(), any()))
            .thenReturn(PageImpl(content.toList(), PageRequest.of(0, 20), content.size.toLong()))
    }

    /**
     * Valideringsmeldingene er kontrakten frontend viser brukeren, så validatoren må være ekte.
     * Den bygges slik ConfigurationValidatorFactory gjør det i drift - med en
     * ConfigurationValidationContext som payload - bare uten oppslagene mot integration og discovery som ellers fyller den.
     */
    private fun stubRealValidator() {
        whenever(configurationValidatorFactory.getValidator(any(), any())).thenReturn(realValidator())
    }

    private fun realValidator(): Validator =
        Validation
            .byProvider(HibernateValidator::class.java)
            .configure()
            .buildValidatorFactory()
            .unwrap(HibernateValidatorFactory::class.java)
            .usingContext()
            .constraintValidatorPayload(
                ConfigurationValidationContext
                    .builder()
                    .integration(integrationDto())
                    .metadata(integrationMetadataDto())
                    .build(),
            ).validator

    private fun configuration(
        id: Long = 1L,
        integrationId: Long = 10L,
        integrationMetadataId: Long = 100L,
        completed: Boolean = false,
        comment: String? = "Kommentar",
        version: Int = 1,
        mapping: ObjectMappingDto? = mappingTree(),
    ) = ConfigurationDto(
        id = id,
        integrationId = integrationId,
        integrationMetadataId = integrationMetadataId,
        completed = completed,
        comment = comment,
        version = version,
        mapping = mapping,
        createdAt = CREATED_AT,
        createdBy = FIRST_ACTOR_OID.toString(),
        createdByActor = Actor.User(FIRST_ACTOR_OID),
        lastModifiedAt = LAST_MODIFIED_AT,
        lastModifiedBy = SECOND_ACTOR_OID.toString(),
        lastModifiedByActor = Actor.User(SECOND_ACTOR_OID),
    )

    private fun mappingTree() =
        ObjectMappingDto(
            valueMappingPerKey =
                mutableMapOf(
                    "tittel" to ValueMappingDto(type = ValueMapping.Type.STRING, mappingString = "Sakstittel"),
                ),
        )

    private fun snapshot(
        id: Long,
        integrationId: Long = 10L,
        integrationMetadataId: Long = 100L,
        version: Int = 1,
        completed: Boolean = false,
        comment: String? = null,
    ) = ConfigurationSnapshot(
        id = id,
        integrationId = integrationId,
        integrationMetadataId = integrationMetadataId,
        version = version,
        completed = completed,
        comment = comment,
    )

    companion object {
        private val OBJECT_MAPPER = FixtureObjectMapper.springBoot()
        private val CREATED_AT: Instant = Instant.parse("2026-01-15T09:00:00Z")
        private val LAST_MODIFIED_AT: Instant = Instant.parse("2026-02-20T13:30:00Z")
        private val FIRST_ACTOR_OID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val SECOND_ACTOR_OID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

        @JvmStatic
        fun httpContractFixtures(): List<HttpContractFixture> = CatalogContractFixtures.http("configuration")
    }
}
