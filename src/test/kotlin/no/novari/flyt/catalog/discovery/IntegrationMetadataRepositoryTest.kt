package no.novari.flyt.catalog.discovery

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.CatalogIntegrationTest
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * Var en `@DataJpaTest`-slice i discovery-service. Slicen kan ikke uttrykke fem datakilder, og
 * erstattes derfor av full kontekst mot Testcontainers — som også gir de ekte persistence-bønnene
 * og JPA-auditingen fra `flyt-audit-starter` framfor en test-lokal `@EnableJpaAuditing`.
 */
class IntegrationMetadataRepositoryTest : CatalogIntegrationTest() {
    @Autowired
    private lateinit var integrationMetadataRepository: IntegrationMetadataRepository

    @Autowired
    @Qualifier("discoveryDataSource")
    private lateinit var discoveryDataSource: DataSource

    @AfterEach
    fun clearState() {
        JdbcTemplate(discoveryDataSource).clearDiscovery()
    }

    @Test
    fun `returns empty collection when source application has no entities`() {
        integrationMetadataRepository.save(createIntegrationMetadata(2L, "TEST-1", 1L))

        val result = integrationMetadataRepository.findAllWithLatestVersionsForSourceApplication(1L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns only latest versions for a source application`() {
        integrationMetadataRepository.saveAll(
            listOf(
                createIntegrationMetadata(1L, "TEST-1", 1L),
                createIntegrationMetadata(1L, "TEST-1", 3L),
                createIntegrationMetadata(1L, "TEST-1", 2L),
                createIntegrationMetadata(1L, "TEST-2", 1L),
                createIntegrationMetadata(2L, "TEST-1", 1L),
            ),
        )

        val result = integrationMetadataRepository.findAllWithLatestVersionsForSourceApplication(1L)
        val resultList = result.sortedBy { it.sourceApplicationIntegrationId }

        assertEquals(2, result.size)
        assertEquals("TEST-1", resultList[0].sourceApplicationIntegrationId)
        assertEquals(3L, resultList[0].version)
        assertEquals("TEST-2", resultList[1].sourceApplicationIntegrationId)
        assertEquals(1L, resultList[1].version)
    }

    @Test
    fun `returns only latest versions for multiple source applications`() {
        integrationMetadataRepository.saveAll(
            listOf(
                createIntegrationMetadata(1L, "TEST-1", 1L),
                createIntegrationMetadata(1L, "TEST-1", 2L),
                createIntegrationMetadata(1L, "TEST-2", 1L),
                createIntegrationMetadata(2L, "TEST-1", 1L),
                createIntegrationMetadata(2L, "TEST-1", 3L),
                createIntegrationMetadata(3L, "TEST-1", 5L),
            ),
        )

        val result =
            integrationMetadataRepository.findAllWithLatestVersionsForSourceApplications(listOf(1L, 2L))
        val resultList =
            result.sortedWith(
                compareBy<IntegrationMetadata> { it.sourceApplicationId }
                    .thenBy { it.sourceApplicationIntegrationId },
            )

        assertEquals(3, result.size)
        assertEquals(1L, resultList[0].sourceApplicationId)
        assertEquals("TEST-1", resultList[0].sourceApplicationIntegrationId)
        assertEquals(2L, resultList[0].version)
        assertEquals(1L, resultList[1].sourceApplicationId)
        assertEquals("TEST-2", resultList[1].sourceApplicationIntegrationId)
        assertEquals(1L, resultList[1].version)
        assertEquals(2L, resultList[2].sourceApplicationId)
        assertEquals("TEST-1", resultList[2].sourceApplicationIntegrationId)
        assertEquals(3L, resultList[2].version)
    }

    @Test
    fun `populates createdAt and createdBy on save`() {
        val saved = integrationMetadataRepository.saveAndFlush(createIntegrationMetadata(1L, "TEST-1", 1L))

        assertEquals(Actor.System, saved.createdBy)
        assertNotNull(saved.createdAt)
    }

    @Test
    fun `rejects a second entity with the same source application, integration and version`() {
        integrationMetadataRepository.saveAndFlush(createIntegrationMetadata(1L, "TEST-1", 1L))

        assertThrows<DataIntegrityViolationException> {
            integrationMetadataRepository.saveAndFlush(createIntegrationMetadata(1L, "TEST-1", 1L))
        }
    }

    @Test
    fun `allows the same source application and integration with a different version`() {
        integrationMetadataRepository.saveAndFlush(createIntegrationMetadata(1L, "TEST-1", 1L))
        integrationMetadataRepository.saveAndFlush(createIntegrationMetadata(1L, "TEST-1", 2L))

        assertEquals(2, integrationMetadataRepository.count())
    }

    private fun createIntegrationMetadata(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
        version: Long,
    ): IntegrationMetadata =
        IntegrationMetadata(
            sourceApplicationId = sourceApplicationId,
            sourceApplicationIntegrationId = sourceApplicationIntegrationId,
            version = version,
            integrationDisplayName = "displayName",
        )
}
