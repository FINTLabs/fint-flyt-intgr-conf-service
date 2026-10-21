package no.novari.flyt.catalog.valueconverting

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Uten `@DataJpaTest` er det ingen transaksjon som rulles tilbake mellom tester, og Envers skriver
 * uansett først ved commit. Radene må derfor ryddes eksplisitt.
 */
fun JdbcTemplate.clearValueConverting() {
    execute(
        """
        truncate table converting_map_aud, value_converting_aud, converting_map, value_converting, revinfo
        restart identity cascade
        """.trimIndent(),
    )
}

fun setAuthenticatedUser(actorId: UUID) {
    val jwt =
        Jwt
            .withTokenValue("test-token")
            .header("alg", "none")
            .claim("objectidentifier", actorId.toString())
            .build()
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = TestingAuthenticationToken(jwt, "credentials", "ROLE_TEST")
    SecurityContextHolder.setContext(context)
}
