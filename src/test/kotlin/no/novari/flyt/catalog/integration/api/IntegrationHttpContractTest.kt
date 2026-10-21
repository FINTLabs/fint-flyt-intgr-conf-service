package no.novari.flyt.catalog.integration.api

import jakarta.validation.Validation
import jakarta.validation.Validator
import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.FixtureObjectMapper
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixture
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixtureRunner
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPatchDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.application.IntegrationUpdateValidationService
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.catalog.integration.validation.IntegrationValidationContext
import no.novari.flyt.catalog.integration.validation.IntegrationValidatorFactory
import no.novari.flyt.catalog.integration.validation.ValidationErrorsFormattingService
import no.novari.flyt.catalog.integration.web.GlobalExceptionHandler
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.validator.HibernateValidator
import org.hibernate.validator.HibernateValidatorFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

/**
 * Fastholder HTTP-kontrakten for integration-domenet slik den er i dag, mot de delte fixturene i
 * `no.novari:flyt-catalog-contract-fixtures`.
 *
 * Valideringen kjører mot en ekte validator, bygget slik IntegrationValidatorFactory gjør det i
 * drift - med en IntegrationValidationContext som payload - bare uten Kafka-oppslaget som ellers
 * fyller den. Meldingene er det frontend viser brukeren, og formatet er derfor kontrakt.
 */
class IntegrationHttpContractTest {
    private lateinit var integrationService: IntegrationService
    private lateinit var integrationValidatorFactory: IntegrationValidatorFactory
    private lateinit var userAuthorizationService: UserAuthorizationService
    private lateinit var authentication: Authentication
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        integrationService = mock()
        integrationValidatorFactory = mock()
        userAuthorizationService = mock()
        authentication = mock()

        val updateValidationService =
            IntegrationUpdateValidationService(
                integrationValidatorFactory,
                ValidationErrorsFormattingService(),
            )

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    IntegrationController(integrationService, updateValidationService, userAuthorizationService),
                ).setControllerAdvice(GlobalExceptionHandler(ValidationErrorsFormattingService()))
                .setMessageConverters(MappingJackson2HttpMessageConverter(OBJECT_MAPPER))
                .setValidator(LocalValidatorFactoryBean().apply { afterPropertiesSet() })
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
    }

    /**
     * Responsen kommer fra stubben, ikke fra det som ble lest inn, så den dekker ikke
     * request-kontrakten: et felt som forsvinner fra DTO-en ville blitt stille ignorert av Jackson.
     */
    private fun verifyDeserializedRequestFor(fixture: HttpContractFixture) {
        when (fixture.id) {
            "integration/post/ok" -> {
                val posted = argumentCaptor<IntegrationPostDto>()
                verify(integrationService).save(posted.capture())

                assertThat(posted.firstValue).usingRecursiveComparison().isEqualTo(
                    IntegrationPostDto(
                        sourceApplicationId = 1L,
                        sourceApplicationIntegrationId = "kildeapp-integrasjon",
                        destination = "arkiv",
                    ),
                )
            }

            "integration/patch/ok-activate" -> {
                val patched = argumentCaptor<IntegrationPatchDto>()
                verify(integrationService).updateById(eq(1L), patched.capture())

                assertThat(patched.firstValue).usingRecursiveComparison().isEqualTo(
                    IntegrationPatchDto(state = Integration.State.ACTIVE, activeConfigurationId = 100L),
                )
            }

            else -> {
                Unit
            }
        }
    }

    private fun stubServiceLayerFor(fixture: HttpContractFixture) {
        when (fixture.id) {
            "integration/list/ok-unpaginated" -> {
                stubAuthorizedSourceApplicationIds()
                whenever(integrationService.findAllBySourceApplicationIds(any()))
                    .thenReturn(listOf(integration()))
            }

            "integration/list/ok-paginated" -> {
                stubAuthorizedSourceApplicationIds()
                whenever(integrationService.findAllBySourceApplicationIds(any(), any()))
                    .thenReturn(PageImpl(listOf(integration()), PageRequest.of(0, 20), 1))
            }

            "integration/list/forbidden-unauthorized-source-application" -> {
                whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(any(), any()))
                    .thenReturn(emptySet())
            }

            "integration/get-by-id/ok" -> {
                whenever(integrationService.findById(1L)).thenReturn(integration())
            }

            "integration/get-by-id/not-found",
            "integration/patch/not-found",
            -> {
                whenever(integrationService.findById(123L)).thenReturn(null)
            }

            "integration/get-by-id/forbidden" -> {
                whenever(integrationService.findById(1L)).thenReturn(integration())
                denyAccessToSourceApplication(1L)
            }

            "integration/post/ok" -> {
                whenever(
                    integrationService.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                        any(),
                        any(),
                    ),
                ).thenReturn(false)
                whenever(integrationService.save(any()))
                    .thenReturn(integration(state = Integration.State.DEACTIVATED, activeConfigurationId = null))
            }

            "integration/post/conflict-already-exists" -> {
                whenever(
                    integrationService.existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                        any(),
                        any(),
                    ),
                ).thenReturn(true)
            }

            "integration/post/unprocessable-missing-fields" -> {
                requestIsRejectedBeforeServiceLayer()
            }

            "integration/patch/ok-activate" -> {
                whenever(integrationService.findById(1L))
                    .thenReturn(integration(state = Integration.State.DEACTIVATED, activeConfigurationId = null))
                stubValidatorWith(completeConfigurationForIntegration())
                whenever(integrationService.updateById(eq(1L), any())).thenReturn(integration())
            }

            "integration/patch/unprocessable-active-without-configuration" -> {
                whenever(integrationService.findById(1L))
                    .thenReturn(integration(state = Integration.State.DEACTIVATED, activeConfigurationId = null))
                stubValidatorWith(configuration = null)
            }

            "integration/patch/unprocessable-configuration-not-found" -> {
                whenever(integrationService.findById(1L)).thenReturn(integration(activeConfigurationId = null))
                stubValidatorWith(configuration = null)
            }

            "integration/patch/unprocessable-configuration-not-for-integration" -> {
                whenever(integrationService.findById(1L)).thenReturn(integration(activeConfigurationId = null))
                stubValidatorWith(completeConfigurationForIntegration(integrationId = 999L))
            }

            "integration/patch/unprocessable-configuration-not-complete" -> {
                whenever(integrationService.findById(1L)).thenReturn(integration(activeConfigurationId = null))
                stubValidatorWith(completeConfigurationForIntegration(completed = false))
            }

            else -> {
                error(
                    "Fixturen '${fixture.id}' har ikke oppsett i denne testen. " +
                        "Legg det til her, ellers er kontrakten udekket i denne tjenesten.",
                )
            }
        }
    }

    /**
     * Fraværet av stubbing er tilsiktet, ikke glemt: @Valid avviser requesten før kontrolleren
     * rører tjenestelaget. En stub her ville aldri blitt kalt.
     */
    private fun requestIsRejectedBeforeServiceLayer() = Unit

    private fun stubAuthorizedSourceApplicationIds() {
        whenever(integrationService.findDistinctSourceApplicationIds()).thenReturn(setOf(1L))
        whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(any(), any()))
            .thenReturn(setOf(1L))
    }

    private fun denyAccessToSourceApplication(sourceApplicationId: Long) {
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application " +
                    "with id=$sourceApplicationId",
            ),
        ).whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(any(), eq(sourceApplicationId))
    }

    private fun stubValidatorWith(configuration: ConfigurationDto?) {
        whenever(integrationValidatorFactory.getPatchValidator(any(), anyOrNull()))
            .thenReturn(validatorWith(configuration))
    }

    private fun validatorWith(configuration: ConfigurationDto?): Validator =
        Validation
            .byProvider(HibernateValidator::class.java)
            .configure()
            .buildValidatorFactory()
            .unwrap(HibernateValidatorFactory::class.java)
            .usingContext()
            .constraintValidatorPayload(
                IntegrationValidationContext(integrationId = 1L, configuration = configuration),
            ).validator

    private fun completeConfigurationForIntegration(
        integrationId: Long = 1L,
        completed: Boolean = true,
    ) = ConfigurationDto(
        id = 100L,
        integrationId = integrationId,
        integrationMetadataId = 1000L,
        completed = completed,
        version = 1,
    )

    private fun integration(
        id: Long = 1L,
        sourceApplicationId: Long = 1L,
        state: Integration.State = Integration.State.ACTIVE,
        activeConfigurationId: Long? = 100L,
    ) = IntegrationDto(
        id = id,
        sourceApplicationId = sourceApplicationId,
        sourceApplicationIntegrationId = "kildeapp-integrasjon",
        destination = "arkiv",
        state = state,
        activeConfigurationId = activeConfigurationId,
        createdAt = CREATED_AT,
        createdBy = FIRST_ACTOR_OID.toString(),
        createdByActor = Actor.User(FIRST_ACTOR_OID),
        lastModifiedAt = LAST_MODIFIED_AT,
        lastModifiedBy = SECOND_ACTOR_OID.toString(),
        lastModifiedByActor = Actor.User(SECOND_ACTOR_OID),
    )

    companion object {
        private val OBJECT_MAPPER = FixtureObjectMapper.springBoot()
        private val CREATED_AT: Instant = Instant.parse("2026-01-15T09:00:00Z")
        private val LAST_MODIFIED_AT: Instant = Instant.parse("2026-02-20T13:30:00Z")
        private val FIRST_ACTOR_OID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val SECOND_ACTOR_OID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

        @JvmStatic
        fun httpContractFixtures(): List<HttpContractFixture> = CatalogContractFixtures.http("integration")
    }
}
