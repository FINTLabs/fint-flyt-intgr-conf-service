package no.novari.flyt.catalog.security

import no.novari.flyt.security.FlytAccess
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class AuthorizationServiceTest {
    private val flytAccess: FlytAccess = mock()
    private val authorizationService = AuthorizationService(flytAccess)

    @Test
    fun `an empty list of source applications needs no call to authorization-service`() {
        assertThat(authorizationService.getUserAuthorizedSourceApplicationIds(emptySet())).isEmpty()
        verifyNoInteractions(flytAccess)
    }

    @Test
    fun `returns the source applications the user has access to`() {
        whenever(flytAccess.sourceApplicationIds()).thenReturn(setOf(1L, 3L))

        assertThat(authorizationService.getUserAuthorizedSourceApplicationIds(setOf(1L, 2L))).containsExactly(1L)
    }
}
