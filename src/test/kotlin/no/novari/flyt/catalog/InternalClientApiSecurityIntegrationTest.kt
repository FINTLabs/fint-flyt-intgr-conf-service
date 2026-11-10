package no.novari.flyt.catalog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestClient
import java.time.Instant

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
    fun `accepts token from intgr-conf client and returns 404 for unknown id`(path: String) {
        givenTokenForClient("test-intgr-conf-client")

        assertThat(get(path, token = "token")).isEqualTo(HttpStatus.NOT_FOUND)
    }

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
        @JvmStatic
        fun endpoints(): List<String> =
            listOf(
                "/api/intern-klient/integrasjoner?sourceApplicationId=999999&sourceApplicationIntegrationId=unknown",
                "/api/intern-klient/integrasjoner/999999999/aktiv-konfigurasjon-id",
                "/api/intern-klient/konfigurasjoner/999999999/mapping",
                "/api/intern-klient/value-convertings/999999999",
            )
    }
}
