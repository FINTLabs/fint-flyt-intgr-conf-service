package no.novari.flyt.catalog

import com.sun.net.httpserver.HttpServer
import no.novari.flyt.catalog.configuration.ConfigurationRepository
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import no.novari.flyt.catalog.configuration.model.configuration.entities.ObjectMapping
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.persistence.IntegrationRepository
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.security.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import tools.jackson.databind.JsonNode
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Runs the frontend's route, `/api/intern`, through the real user chain from flyt-security-starter and the real
 * `FlytAccess`. Only the token decoding is mocked; the chain still checks the user claims. A local HTTP server stands
 * in for NAM's token endpoint and for authorization-service's
 * `GET /api/intern-klient/authorization/users/{objectIdentifier}/source-application-ids` (FFS-2429).
 *
 * Goes over real HTTP, because MockMvc does not forward errors to `/error`.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = [
        "novari.flyt.security.user.audience=$FRONTEND_CLIENT_ID",
        "fint.org-id=$TENANT_ORG_ID",
    ],
)
class InternalApiSecurityIntegrationTest : CatalogIntegrationTest() {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var integrationRepository: IntegrationRepository

    @Autowired
    private lateinit var configurationRepository: ConfigurationRepository

    private val savedConfigurations = mutableListOf<Configuration>()

    @MockitoBean
    lateinit var jwtDecoder: JwtDecoder

    private val saved = mutableListOf<Integration>()

    @BeforeEach
    fun setUp() {
        AuthorizationServiceStub.reset()
        saved += integration(ACCESSIBLE_SOURCE_APPLICATION_ID)
        saved += integration(OTHER_SOURCE_APPLICATION_ID)
    }

    @AfterEach
    fun tearDown() {
        configurationRepository.deleteAll(savedConfigurations.filter { configurationRepository.existsById(it.id!!) })
        savedConfigurations.clear()
        integrationRepository.deleteAll(saved)
        saved.clear()
    }

    @Test
    fun `lists only integrations from the source applications authorization-service gives the user`() {
        val objectIdentifier = UUID.randomUUID().toString()
        givenUserToken(objectIdentifier = objectIdentifier)
        AuthorizationServiceStub.sourceApplicationIds = "[$ACCESSIBLE_SOURCE_APPLICATION_ID]"

        val integrations =
            client()
                .get()
                .uri("/api/intern/integrasjoner?sourceApplicationId=$ACCESSIBLE_SOURCE_APPLICATION_ID")
                .headers { it.setBearerAuth("token") }
                .retrieve()
                .body<List<IntegrationDto>>()

        assertThat(integrations.orEmpty().map { it.sourceApplicationId }).containsOnly(ACCESSIBLE_SOURCE_APPLICATION_ID)
        val lookup = AuthorizationServiceStub.requests.single { it.path != "/token" }
        assertThat(lookup.path)
            .isEqualTo("/api/intern-klient/authorization/users/$objectIdentifier/source-application-ids")
        assertThat(lookup.authorization).isEqualTo("Bearer $INTERNAL_TOKEN")
    }

    @Test
    fun `gives 403 for a source application the user has no access to`() {
        givenUserToken()
        AuthorizationServiceStub.sourceApplicationIds = "[$ACCESSIBLE_SOURCE_APPLICATION_ID]"

        assertThat(get("/api/intern/integrasjoner/${saved.last().id}")).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `gives 503 when authorization-service fails`() {
        givenUserToken()
        AuthorizationServiceStub.status = 500

        assertThat(get("/api/intern/integrasjoner/${saved.first().id}")).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `rejects a token for another audience without asking authorization-service`() {
        givenUserToken(audience = "another-client")

        assertThat(get("/api/intern/integrasjoner")).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(AuthorizationServiceStub.requests).isEmpty()
    }

    @Test
    fun `rejects a user from another organization`() {
        givenUserToken(organizationId = "other.no")

        assertThat(get("/api/intern/integrasjoner")).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `lists and shows the history of only configurations in the user's integrations (FFS-2431)`() {
        val (own, other) = configurations()
        givenUserToken()
        AuthorizationServiceStub.sourceApplicationIds = "[$ACCESSIBLE_SOURCE_APPLICATION_ID]"

        val listed =
            client()
                .get()
                .uri("/api/intern/konfigurasjoner?side=0&antall=1000&sorteringFelt=id&sorteringRetning=ASC")
                .headers { it.setBearerAuth("token") }
                .retrieve()
                .body<JsonNode>()
        val listedIds = requireNotNull(listed)["content"].values().map { it["id"].asLong() }
        assertThat(listedIds).contains(own.id).doesNotContain(other.id)

        val history =
            client()
                .get()
                .uri("/api/intern/konfigurasjoner/history?page=0&size=1000")
                .headers { it.setBearerAuth("token") }
                .retrieve()
                .body<JsonNode>()
        val historyIntegrationIds =
            requireNotNull(history)["content"].values().map { it["snapshot"]["integrationId"].asLong() }
        assertThat(historyIntegrationIds).contains(own.integrationId).doesNotContain(other.integrationId)
    }

    @Test
    fun `gives 403 for a configuration in another source application and deletes nothing (FFS-2431)`() {
        val (_, other) = configurations()
        givenUserToken()
        AuthorizationServiceStub.sourceApplicationIds = "[$ACCESSIBLE_SOURCE_APPLICATION_ID]"

        assertThat(get("/api/intern/konfigurasjoner/${other.id}")).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(get("/api/intern/konfigurasjoner/${other.id}/history?page=0&size=20"))
            .isEqualTo(HttpStatus.FORBIDDEN)
        val deleted =
            client()
                .delete()
                .uri("/api/intern/konfigurasjoner/${other.id}")
                .headers { it.setBearerAuth("token") }
                .exchange { _, response -> response.statusCode }
        assertThat(deleted).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(configurationRepository.existsById(other.id!!)).isTrue()
    }

    /** One configuration in the accessible source application's integration, one in the other's. */
    private fun configurations(): Pair<Configuration, Configuration> {
        val own =
            configurationRepository.save(
                Configuration(integrationId = saved.first().id, integrationMetadataId = 1L, mapping = ObjectMapping()),
            )
        val other =
            configurationRepository.save(
                Configuration(integrationId = saved.last().id, integrationMetadataId = 1L, mapping = ObjectMapping()),
            )
        savedConfigurations += listOf(own, other)
        return own to other
    }

    @Test
    fun `rejects a request without token`() {
        assertThat(get("/api/intern/integrasjoner", token = null)).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    private fun integration(sourceApplicationId: Long): Integration =
        integrationRepository.save(
            Integration(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationIntegrationId = "security-test-${UUID.randomUUID()}",
                destination = "test",
                state = Integration.State.DEACTIVATED,
            ),
        )

    private fun givenUserToken(
        objectIdentifier: String = UUID.randomUUID().toString(),
        audience: String = FRONTEND_CLIENT_ID,
        organizationId: String = TENANT_ORG_ID,
    ) {
        whenever(jwtDecoder.decode(any())).thenReturn(
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject("opaque-user-id")
                .audience(listOf(audience))
                .claim("scope", "end-user-profile fint-api")
                .claim("organizationid", organizationId)
                .claim("objectidentifier", objectIdentifier)
                .claim("roles", listOf(UserRole.USER.claimValue))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build(),
        )
    }

    private fun client() = RestClient.create("http://localhost:$port")

    private fun get(
        path: String,
        token: String? = "token",
    ): HttpStatusCode =
        client()
            .get()
            .uri(path)
            .headers { headers -> token?.let { headers.setBearerAuth(it) } }
            .exchange { _, response -> response.statusCode }
            ?: error("Expected an HTTP status from the internal API")

    /** NAM's token endpoint and authorization-service, on one local port. */
    private object AuthorizationServiceStub {
        data class Request(
            val path: String,
            val authorization: String?,
        )

        val requests = CopyOnWriteArrayList<Request>()

        @Volatile
        var sourceApplicationIds = "[]"

        @Volatile
        var status = 200

        val baseUrl: String by lazy {
            val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
            server.createContext("/") { exchange ->
                requests += Request(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"))
                val (code, body) =
                    if (exchange.requestURI.path == "/token") {
                        200 to """{"access_token":"$INTERNAL_TOKEN","token_type":"Bearer","expires_in":300}"""
                    } else {
                        status to sourceApplicationIds
                    }
                val bytes = body.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(code, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
            "http://localhost:${server.address.port}"
        }

        fun reset() {
            requests.clear()
            sourceApplicationIds = "[]"
            status = 200
        }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun authorizationServiceProperties(registry: DynamicPropertyRegistry) {
            registry.add("novari.flyt.security.authorization.base-url") { AuthorizationServiceStub.baseUrl }
            registry.add("spring.security.oauth2.client.provider.fint-idp.token-uri") {
                "${AuthorizationServiceStub.baseUrl}/token"
            }
        }
    }
}

private const val FRONTEND_CLIENT_ID = "frontend-client"
private const val TENANT_ORG_ID = "fintlabs.no"
private const val INTERNAL_TOKEN = "internal-client-token"
private const val ACCESSIBLE_SOURCE_APPLICATION_ID = 9001L
private const val OTHER_SOURCE_APPLICATION_ID = 9002L
