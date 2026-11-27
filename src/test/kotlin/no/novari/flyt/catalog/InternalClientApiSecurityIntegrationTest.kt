package no.novari.flyt.catalog

import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.JsonPath
import no.novari.flyt.catalog.discovery.IntegrationMetadataRepository
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.persistence.IntegrationRepository
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Runs the real security filter chain from flyt-web-resource-server against `/api/intern-klient`.
 * Only the token decoding is mocked; the subject of the decoded token is the OAuth client id.
 *
 * Goes over real HTTP, because MockMvc does not forward errors to `/error`, where the security
 * filter chain turns a 404 into a 401.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternalClientApiSecurityIntegrationTest : CatalogIntegrationTest() {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var metadataRepository: IntegrationMetadataRepository

    @Autowired
    private lateinit var integrationRepository: IntegrationRepository

    @MockitoBean
    lateinit var jwtDecoder: JwtDecoder

    @ParameterizedTest
    @MethodSource("endpoints")
    fun `rejects request without token`(path: String) {
        assertThat(get(path, token = null)).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    fun `rejects token from another client`(path: String) {
        givenTokenForClient("fint-flyt-some-other-oauth2-client")

        assertThat(get(path, token = "token")).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    fun `accepts token from internal client and returns 404 for unknown id`(path: String) {
        givenTokenForClient("test-client")

        assertThat(get(path, token = "token")).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @ParameterizedTest
    @EnumSource(Integration.State::class)
    fun `returns the state of a known integration`(state: Integration.State) {
        givenTokenForClient("test-client")
        val integrationId = "conexus-test-${UUID.randomUUID()}"
        val saved =
            integrationRepository.save(
                Integration(
                    sourceApplicationId = 1,
                    sourceApplicationIntegrationId = integrationId,
                    destination = "test",
                    state = state,
                    activeConfigurationId = if (state == Integration.State.ACTIVE) 100L else null,
                ),
            )
        try {
            val response =
                RestClient
                    .create("http://localhost:$port")
                    .get()
                    .uri(
                        "/api/intern-klient/integrasjoner?sourceApplicationId=1&sourceApplicationIntegrationId={integrationId}",
                        integrationId,
                    ).headers { it.setBearerAuth("token") }
                    .retrieve()
                    .toEntity(IntegrationDto::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val integration = requireNotNull(response.body)
            assertThat(integration.id).isEqualTo(saved.id)
            assertThat(integration.sourceApplicationIntegrationId).isEqualTo(integrationId)
            assertThat(integration.state).isEqualTo(state)
            assertThat(integration.activeConfigurationId).isEqualTo(saved.activeConfigurationId)
        } finally {
            integrationRepository.delete(saved)
        }
    }

    @Test
    fun `reads only the highest metadata version for the requested source application and integration`() {
        givenTokenForClient("test-client")
        val integrationId = "conexus-test-${UUID.randomUUID()}"
        val otherIntegrationId = "$integrationId-other"

        try {
            assertThat(postMetadata(integrationId, 10, token = "token").statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(postMetadata(integrationId, 2, token = "token").statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(postMetadata(integrationId, 11, token = "token", sourceApplicationId = 2).statusCode)
                .isEqualTo(HttpStatus.CREATED)
            assertThat(postMetadata(otherIntegrationId, 12, token = "token").statusCode).isEqualTo(HttpStatus.CREATED)

            val response =
                RestClient
                    .create("http://localhost:$port")
                    .get()
                    .uri(
                        "$METADATA_PATH?sourceApplicationId=1&sourceApplicationIntegrationId={integrationId}",
                        integrationId,
                    ).headers { it.setBearerAuth("token") }
                    .retrieve()
                    .toEntity(IntegrationMetadataDto::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val metadata = requireNotNull(response.body)
            assertThat(metadata.sourceApplicationId).isEqualTo(1L)
            assertThat(metadata.sourceApplicationIntegrationId).isEqualTo(integrationId)
            assertThat(metadata.version).isEqualTo(10L)
            val instanceMetadata = requireNotNull(metadata.instanceMetadata)
            val field = instanceMetadata.instanceValueMetadata.single()
            assertThat(field.key).isEqualTo("elevNavn")
            assertThat(field.type).isEqualTo(InstanceValueMetadata.Type.STRING)
            val category = instanceMetadata.categories.single()
            assertThat(category.displayName).isEqualTo("Hoveddokument")
            val file = requireNotNull(category.content).instanceValueMetadata.single()
            assertThat(file.key).isEqualTo("hovedDokumentFil")
            assertThat(file.type).isEqualTo(InstanceValueMetadata.Type.FILE)
        } finally {
            metadataRepository.deleteAll(registeredVersions(integrationId))
            metadataRepository.deleteAll(registeredVersions(integrationId, sourceApplicationId = 2))
            metadataRepository.deleteAll(registeredVersions(otherIntegrationId))
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "?sourceApplicationId=1",
            "?sourceApplicationIntegrationId=kartlegging",
            "?sourceApplicationId=invalid&sourceApplicationIntegrationId=kartlegging",
            "?sourceApplicationId=1&sourceApplicationIntegrationId=",
        ],
    )
    fun `rejects missing or invalid metadata lookup parameters`(query: String) {
        givenTokenForClient("test-client")

        assertThat(get("$METADATA_PATH$query", token = "token")).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `rejects an invalid token for metadata lookup`() {
        whenever(jwtDecoder.decode(any())).thenThrow(BadJwtException("Invalid token"))

        assertThat(get(UNKNOWN_METADATA_PATH, token = "invalid")).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `documents metadata lookup errors with the correct OpenAPI response bodies`() {
        val openApi =
            RestClient
                .create("http://localhost:$port")
                .get()
                .uri("/v3/api-docs/discovery")
                .retrieve()
                .body(JsonNode::class.java)
        val responses =
            requireNotNull(openApi)
                .path("paths")
                .path(METADATA_PATH)
                .path("get")
                .path("responses")

        for (code in listOf("400", "404")) {
            val content = responses.path(code).path("content")
            assertThat(content.size()).isEqualTo(1)
            val schema = content.path(MediaType.APPLICATION_PROBLEM_JSON_VALUE).path("schema")
            assertThat(schema.path("\$ref").asText()).isEqualTo("#/components/schemas/ProblemDetail")
        }
        for (code in listOf("401", "403")) {
            assertThat(requireNotNull(responses[code]).has("content")).isFalse()
        }
    }

    @Test
    fun `limits metadata registration to the internal client and rejects a duplicate version`() {
        val integrationId = "conexus-test-${UUID.randomUUID()}"
        assertThat(postMetadata(integrationId, 1, token = null).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        givenTokenForClient("fint-flyt-some-other-oauth2-client")
        assertThat(postMetadata(integrationId, 1, token = "token").statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        givenTokenForClient("test-client")
        try {
            val invalid = postMetadata(integrationId, null, token = "token")
            assertThat(problemDetail(invalid, HttpStatus.BAD_REQUEST)).isNotBlank()
            assertThat(registeredVersions(integrationId)).isEmpty()
            val barrier = CyclicBarrier(2)
            Executors.newFixedThreadPool(2).use { executor ->
                val requests =
                    List(2) {
                        executor.submit<Int> {
                            barrier.await(5, TimeUnit.SECONDS)
                            postMetadata(integrationId, 1, token = "token").statusCode.value()
                        }
                    }
                assertThat(requests.map { it.get(15, TimeUnit.SECONDS) })
                    .containsExactlyInAnyOrder(201, 409)
            }
            assertThat(postMetadata(integrationId, 2, token = "token").statusCode).isEqualTo(HttpStatus.CREATED)
            val duplicate = postMetadata(integrationId, 1, token = "token")
            assertThat(problemDetail(duplicate, HttpStatus.CONFLICT)).isEqualTo("Metadata version already exists")
            assertThat(registeredVersions(integrationId).map { it.version }).containsExactlyInAnyOrder(1L, 2L)
        } finally {
            metadataRepository.deleteAll(registeredVersions(integrationId))
        }
    }

    private fun problemDetail(
        response: ResponseEntity<String>,
        status: HttpStatus,
    ): String {
        assertThat(response.statusCode).isEqualTo(status)
        val contentType = requireNotNull(response.headers.contentType)
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue()
        return JsonPath.read(requireNotNull(response.body), "$.detail")
    }

    private fun registeredVersions(
        integrationId: String,
        sourceApplicationId: Long = 1,
    ) = metadataRepository
        .findAllBySourceApplicationIdAndSourceApplicationIntegrationId(sourceApplicationId, integrationId)

    private fun postMetadata(
        integrationId: String,
        version: Long?,
        token: String?,
        sourceApplicationId: Long = 1,
    ): ResponseEntity<String> =
        RestClient
            .create("http://localhost:$port")
            .post()
            .uri(METADATA_PATH)
            .headers { headers -> token?.let { headers.setBearerAuth(it) } }
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """{
                      "sourceApplicationId":$sourceApplicationId,
                      "sourceApplicationIntegrationId":"$integrationId",
                      "integrationDisplayName":"Test",
                      "version":$version,
                      "instanceMetadata":{
                        "instanceValueMetadata":[
                          {"displayName":"Elevens navn","type":"STRING","key":"elevNavn"}
                        ],
                        "categories":[
                          {
                            "displayName":"Hoveddokument",
                            "content":{
                              "instanceValueMetadata":[
                                {"displayName":"Hoveddokument","type":"FILE","key":"hovedDokumentFil"}
                              ]
                            }
                          }
                        ]
                      }
                    }""",
            ).retrieve()
            .onStatus(HttpStatusCode::isError) { _, _ -> }
            .toEntity(String::class.java)

    private fun get(
        path: String,
        token: String?,
    ): HttpStatusCode =
        RestClient
            .create("http://localhost:$port")
            .get()
            .uri(path)
            .headers { headers -> token?.let { headers.setBearerAuth(it) } }
            .exchange { _, response -> response.statusCode }
            ?: error("Expected an HTTP status from the internal API")

    private fun givenTokenForClient(clientId: String) {
        whenever(jwtDecoder.decode(any())).thenReturn(
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject(clientId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build(),
        )
    }

    companion object {
        private const val METADATA_PATH = "/api/intern-klient/metadata"
        private const val UNKNOWN_METADATA_PATH =
            "$METADATA_PATH?sourceApplicationId=999999&sourceApplicationIntegrationId=unknown"

        @JvmStatic
        fun endpoints(): List<String> =
            listOf(
                "/api/intern-klient/integrasjoner?sourceApplicationId=999999&sourceApplicationIntegrationId=unknown",
                "/api/intern-klient/integrasjoner/999999999/aktiv-konfigurasjon-id",
                "/api/intern-klient/konfigurasjoner/999999999/mapping",
                "/api/intern-klient/value-convertings/999999999",
                UNKNOWN_METADATA_PATH,
            )
    }
}
