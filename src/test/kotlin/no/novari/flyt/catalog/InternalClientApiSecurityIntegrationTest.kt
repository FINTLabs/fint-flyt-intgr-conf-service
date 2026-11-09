package no.novari.flyt.catalog

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * Runs the real security filter chain from flyt-web-resource-server against `/api/intern-klient`.
 * Only the token decoding is mocked; the subject of the decoded token is the OAuth client id.
 */
@AutoConfigureMockMvc
class InternalClientApiSecurityIntegrationTest : CatalogIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var jwtDecoder: JwtDecoder

    @ParameterizedTest
    @MethodSource("endpoints")
    fun `rejects request without token`(path: String) {
        mockMvc
            .perform(get(path))
            .andExpect(status().isUnauthorized)
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    fun `rejects token from another client`(path: String) {
        givenTokenForClient("fint-flyt-some-other-oauth2-client")

        mockMvc
            .perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer token"))
            .andExpect(status().isForbidden)
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    fun `accepts token from intgr-conf client and returns 404 for unknown id`(path: String) {
        givenTokenForClient("test-intgr-conf-client")

        mockMvc
            .perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer token"))
            .andExpect(status().isNotFound)
    }

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
