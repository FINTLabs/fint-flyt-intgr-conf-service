package no.novari.flyt.catalog.discovery

import jakarta.validation.Validation
import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.FixtureObjectMapper
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixture
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixtureRunner
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataCategoryDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceObjectCollectionMetadataDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceValueMetadataDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import no.novari.flyt.catalog.discovery.validation.ValidationErrorsFormattingService
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID

/**
 * Fastholder HTTP-kontrakten for discovery-domenet slik den er i dag, mot de delte fixturene i
 * `no.novari:flyt-catalog-contract-fixtures`.
 *
 * Flaten er uvanlig ved at fire GET-varianter deler samme path og diskrimineres på hvilke
 * query-parametre som er til stede. Fixturene dekker alle fire, inkludert kombinasjonen som er
 * eksplisitt avvist.
 */
class IntegrationMetadataHttpContractTest {
    private lateinit var integrationMetadataService: IntegrationMetadataService
    private lateinit var userAuthorizationService: UserAuthorizationService
    private lateinit var authentication: Authentication
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        integrationMetadataService = mock()
        userAuthorizationService = mock()
        authentication = mock()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    IntegrationMetadataController(
                        integrationMetadataService,
                        Validation.buildDefaultValidatorFactory().validator,
                        ValidationErrorsFormattingService(),
                        userAuthorizationService,
                    ),
                ).setControllerAdvice(IntegrationMetadataRequestExceptionHandler())
                .setMessageConverters(MappingJackson2HttpMessageConverter(OBJECT_MAPPER))
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
     * Responsen dekker ikke request-kontrakten på denne flaten: POST svarer med tom body, så et felt
     * som forsvinner fra IntegrationMetadataDto ville blitt stille ignorert av Jackson og gitt 200
     * likevel. Sammenligningen er rekursiv fordi hele treet er kontrakt - gateways bygget på
     * flyt-web-instance-gateway sender det.
     */
    private fun verifyDeserializedRequestFor(fixture: HttpContractFixture) {
        if (fixture.id != "discovery/post/ok") {
            return
        }

        val posted = argumentCaptor<IntegrationMetadataDto>()
        verify(integrationMetadataService).save(posted.capture())

        assertThat(posted.firstValue)
            .usingRecursiveComparison()
            .isEqualTo(postedMetadata())
    }

    private fun postedMetadata() =
        IntegrationMetadataDto(
            sourceApplicationId = 1L,
            sourceApplicationIntegrationId = "kildeapp-integrasjon",
            sourceApplicationIntegrationUri = "https://kildeapp.example/integrasjon/1",
            integrationDisplayName = "Byggesak",
            version = 1L,
            instanceMetadata =
                InstanceMetadataContentDto(
                    instanceValueMetadata = listOf(value("Tittel", InstanceValueMetadata.Type.STRING, "tittel")),
                ),
        )

    private fun stubServiceLayerFor(fixture: HttpContractFixture) {
        when (fixture.id) {
            "discovery/metadata-by-source-application/ok" -> {
                whenever(
                    integrationMetadataService.getIntegrationMetadataForSourceApplication(
                        sourceApplicationId = eq(1L),
                        onlyLatestVersions = eq(false),
                    ),
                ).thenReturn(listOf(metadata()))
            }

            "discovery/metadata-by-source-application/ok-only-latest" -> {
                whenever(
                    integrationMetadataService.getIntegrationMetadataForSourceApplication(
                        sourceApplicationId = eq(1L),
                        onlyLatestVersions = eq(true),
                    ),
                ).thenReturn(listOf(metadata()))
            }

            "discovery/metadata-by-integration/ok" -> {
                whenever(
                    integrationMetadataService.getAllForSourceApplicationIdAndSourceApplicationIntegrationId(
                        sourceApplicationId = eq(1L),
                        sourceApplicationIntegrationId = eq("kildeapp-integrasjon"),
                    ),
                ).thenReturn(listOf(metadata()))
            }

            "discovery/metadata-by-source-applications/ok" -> {
                whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(any(), any()))
                    .thenReturn(setOf(1L))
                whenever(
                    integrationMetadataService.getIntegrationMetadataForSourceApplications(
                        sourceApplicationIds = any(),
                        onlyLatestVersions = eq(false),
                    ),
                ).thenReturn(mapOf(1L to listOf(metadata())))
            }

            "discovery/metadata-unsupported-combination/bad-request" -> {
                combinationIsRejectedByRouting()
            }

            "discovery/instance-metadata-by-id/ok" -> {
                whenever(integrationMetadataService.getById(1L)).thenReturn(metadata())
            }

            "discovery/instance-metadata-by-id/not-found" -> {
                whenever(integrationMetadataService.getById(123L)).thenReturn(null)
            }

            "discovery/post/ok" -> {
                whenever(integrationMetadataService.versionExists(any())).thenReturn(false)
                whenever(integrationMetadataService.save(any())).thenReturn(metadata())
            }

            "discovery/post/conflict-version-exists" -> {
                whenever(integrationMetadataService.versionExists(any())).thenReturn(true)
            }

            "discovery/post/unprocessable-validation-errors" -> {
                requestIsRejectedBeforeServiceLayer()
            }

            else -> {
                error(
                    "Fixturen '${fixture.id}' har ikke oppsett i denne testen. " +
                        "Legg det til her, ellers er kontrakten udekket i denne tjenesten.",
                )
            }
        }
    }

    /** Handleren for denne parameterkombinasjonen svarer 400 uten å røre tjenestelaget. */
    private fun combinationIsRejectedByRouting() = Unit

    /** Valideringen avviser requesten før kontrolleren rører tjenestelaget. */
    private fun requestIsRejectedBeforeServiceLayer() = Unit

    private fun metadata() =
        IntegrationMetadataDto(
            id = 1L,
            sourceApplicationId = 1L,
            sourceApplicationIntegrationId = "kildeapp-integrasjon",
            sourceApplicationIntegrationUri = "https://kildeapp.example/integrasjon/1",
            integrationDisplayName = "Byggesak",
            version = 1L,
            instanceMetadata = instanceMetadata(),
            createdAt = Instant.parse("2026-01-15T09:00:00Z"),
            createdBy = ACTOR_OID.toString(),
            createdByActor = Actor.User(ACTOR_OID),
        )

    private fun instanceMetadata() =
        InstanceMetadataContentDto(
            instanceValueMetadata =
                listOf(
                    value("Tittel", InstanceValueMetadata.Type.STRING, "tittel"),
                    value("Er hastesak", InstanceValueMetadata.Type.BOOLEAN, "erHastesak"),
                    value("Vedlegg", InstanceValueMetadata.Type.FILE, "vedlegg"),
                ),
            instanceObjectCollectionMetadata =
                listOf(
                    InstanceObjectCollectionMetadataDto(
                        displayName = "Dokumenter",
                        key = "dokumenter",
                        objectMetadata =
                            InstanceMetadataContentDto(
                                instanceValueMetadata =
                                    listOf(value("Filnavn", InstanceValueMetadata.Type.STRING, "filnavn")),
                            ),
                    ),
                ),
            categories =
                listOf(
                    InstanceMetadataCategoryDto(
                        displayName = "Avsender",
                        content =
                            InstanceMetadataContentDto(
                                instanceValueMetadata =
                                    listOf(value("Navn", InstanceValueMetadata.Type.STRING, "navn")),
                            ),
                    ),
                ),
        )

    private fun value(
        displayName: String,
        type: InstanceValueMetadata.Type,
        key: String,
    ) = InstanceValueMetadataDto(displayName = displayName, type = type, key = key)

    companion object {
        private val OBJECT_MAPPER = FixtureObjectMapper.springBoot()
        private val ACTOR_OID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

        @JvmStatic
        fun httpContractFixtures(): List<HttpContractFixture> = CatalogContractFixtures.http("discovery")
    }
}
