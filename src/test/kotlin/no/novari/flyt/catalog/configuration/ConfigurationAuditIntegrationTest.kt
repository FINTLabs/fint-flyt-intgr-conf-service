package no.novari.flyt.catalog.configuration

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.audit.history.HistoryEventType
import no.novari.flyt.catalog.CatalogIntegrationTest
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationPatchDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Kjører mot en ekte Postgres slik at Flyway-migrasjonene, JSONB-aktørkolonnene og
 * Envers-revisjonene faktisk blir utøvd — `ddl-auto: none` gjør at skjemaet kommer fra
 * migrasjonene, ikke fra Hibernate.
 *
 * Navneoppslaget mot authorization-service er ikke tilgjengelig, og konteksten deles med de andre
 * domenenes tester og har derfor driftsoppsettet, der `novari.flyt.audit.display.unknown-user` ikke
 * er satt. Visningsnavnet for en bruker som ikke kan slås opp, er dermed `null`.
 */
class ConfigurationAuditIntegrationTest : CatalogIntegrationTest() {
    @Autowired
    lateinit var configurationService: ConfigurationService

    @Autowired
    lateinit var configurationHistoryService: ConfigurationHistoryService

    @Autowired
    @Qualifier("configurationDataSource")
    lateinit var configurationDataSource: DataSource

    @Autowired
    @Qualifier("configurationTransactionManager")
    lateinit var transactionManager: PlatformTransactionManager

    private val jdbcTemplate: JdbcTemplate get() = JdbcTemplate(configurationDataSource)

    private val transactionTemplate: TransactionTemplate by lazy { TransactionTemplate(transactionManager) }

    private val userOid: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        jdbcTemplate.clearConfiguration()
        setJwtWithOid(userOid)
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `save populates audit fields with the authenticated actor`() {
        val saved = configurationService.save(configurationDto("v1"))

        assertThat(saved.createdAt).isNotNull()
        assertThat(saved.createdByActor).isEqualTo(Actor.User(userOid))
        assertThat(saved.createdBy).isNull()
        assertThat(saved.lastModifiedAt).isNotNull()
        assertThat(saved.lastModifiedByActor).isEqualTo(Actor.User(userOid))
    }

    @Test
    fun `history returns newest revision first with metadata snapshot`() {
        val id = requireNotNull(configurationService.save(configurationDto("v1")).id)
        configurationService.updateById(id, ConfigurationPatchDto(comment = "endret"))

        val page = findHistory(id)

        assertThat(page.totalElements).isEqualTo(2)
        assertThat(page.content.map { it.type })
            .containsExactly(HistoryEventType.UPDATED, HistoryEventType.CREATED)
        assertThat(page.content.map { it.snapshot?.comment }).containsExactly("endret", null)
        assertThat(page.content).allSatisfy {
            assertThat(it.actor).isEqualTo(Actor.User(userOid))
            assertThat(it.actorDisplay).isNull()
        }
    }

    /**
     * `mapping` er `targetAuditMode = NOT_AUDITED`, så gamle revisjoner peker på en `object_mapping`-rad
     * som er orphan-removed. Relasjonen er en proxy som aldri dereferereres av det metadata-baserte
     * snapshotet — legges mapping inn i [ConfigurationSnapshot] senere, feiler denne testen med
     * EntityNotFoundException, og det er med vilje.
     */
    @Test
    fun `history survives a mapping replacement that orphan removed the previous tree`() {
        val id = requireNotNull(configurationService.save(configurationDto("v1")).id)
        val replacedMappingId =
            jdbcTemplate.queryForObject(
                "select mapping_id from configuration where id = ?",
                Long::class.java,
                id,
            )

        configurationService.updateById(id, ConfigurationPatchDto(mapping = objectMappingDto("v2")))

        val remainingRowsForReplacedMapping: Long? =
            jdbcTemplate.queryForObject(
                "select count(*) from object_mapping where id = ?",
                Long::class.java,
                requireNotNull(replacedMappingId),
            )
        assertThat(remainingRowsForReplacedMapping).isEqualTo(0L)

        val page = findHistory(id)

        assertThat(page.totalElements).isEqualTo(2)
        assertThat(page.content.map { it.type })
            .containsExactly(HistoryEventType.UPDATED, HistoryEventType.CREATED)
    }

    @Test
    fun `delete gives a DELETED entry with null snapshot`() {
        val id = requireNotNull(configurationService.save(configurationDto("v1")).id)

        configurationService.deleteById(id)

        val page = findHistory(id)

        assertThat(page.content.first().type).isEqualTo(HistoryEventType.DELETED)
        assertThat(page.content.first().snapshot).isNull()
    }

    @Test
    fun `history without authentication records the system actor`() {
        SecurityContextHolder.clearContext()

        val id = requireNotNull(configurationService.save(configurationDto("v1")).id)

        val page = findHistory(id)

        assertThat(page.content.first().actor).isEqualTo(Actor.System)
        assertThat(page.content.first().actorDisplay).isEqualTo("System")
    }

    @Test
    fun `findAll hydrates actor display names for every row on the page`() {
        configurationService.save(configurationDto("v1"))
        configurationService.save(configurationDto("v2"))

        val page = configurationService.findAll(ConfigurationFilter(null, null), true, PageRequest.of(0, 20))

        assertThat(page.totalElements).isEqualTo(2)
        assertThat(page.content).allSatisfy {
            assertThat(it.createdBy).isNull()
            assertThat(it.createdByActor).isEqualTo(Actor.User(userOid))
        }
    }

    /** Envers' AuditReader krever en åpen EntityManager, altså en aktiv transaksjon. */
    private fun findHistory(id: Long) =
        requireNotNull(
            transactionTemplate.execute {
                configurationHistoryService.findHistory(id, PageRequest.of(0, 20))
            },
        )

    private fun configurationDto(mappingString: String): ConfigurationDto =
        ConfigurationDto
            .builder()
            .integrationId(1L)
            .integrationMetadataId(2L)
            .mapping(objectMappingDto(mappingString))
            .build()

    private fun objectMappingDto(mappingString: String): ObjectMappingDto =
        ObjectMappingDto
            .builder()
            .valueMappingPerKey(
                mutableMapOf(
                    "felt" to
                        ValueMappingDto
                            .builder()
                            .type(ValueMapping.Type.STRING)
                            .mappingString(mappingString)
                            .build(),
                ),
            ).build()

    private fun setJwtWithOid(oid: UUID) {
        val jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .claim("objectidentifier", oid.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(jwt, null)
    }
}
