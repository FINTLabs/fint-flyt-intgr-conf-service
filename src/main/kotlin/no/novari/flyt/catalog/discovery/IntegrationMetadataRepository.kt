package no.novari.flyt.catalog.discovery

import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface IntegrationMetadataRepository : JpaRepository<IntegrationMetadata, Long> {
    @Query(
        """
        SELECT im
        FROM IntegrationMetadata im
        WHERE im.sourceApplicationId = :sourceApplicationId
          AND im.version IN (
            SELECT MAX(iim.version)
            FROM IntegrationMetadata iim
            WHERE im.sourceApplicationId = iim.sourceApplicationId
              AND im.sourceApplicationIntegrationId = iim.sourceApplicationIntegrationId
          )
        """,
    )
    fun findAllWithLatestVersionsForSourceApplication(
        @Param("sourceApplicationId") sourceApplicationId: Long,
    ): List<IntegrationMetadata>

    @Query(
        """
        SELECT im
        FROM IntegrationMetadata im
        WHERE im.sourceApplicationId IN :sourceApplicationIds
          AND im.version IN (
            SELECT MAX(iim.version)
            FROM IntegrationMetadata iim
            WHERE im.sourceApplicationId = iim.sourceApplicationId
              AND im.sourceApplicationIntegrationId = iim.sourceApplicationIntegrationId
          )
        """,
    )
    fun findAllWithLatestVersionsForSourceApplications(
        @Param("sourceApplicationIds") sourceApplicationIds: Collection<Long>,
    ): List<IntegrationMetadata>

    fun findAllBySourceApplicationId(sourceApplicationId: Long): List<IntegrationMetadata>

    fun findAllBySourceApplicationIdIn(sourceApplicationIds: Collection<Long>): List<IntegrationMetadata>

    fun findAllBySourceApplicationIdAndSourceApplicationIntegrationId(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
    ): List<IntegrationMetadata>

    fun existsBySourceApplicationIdAndSourceApplicationIntegrationIdAndVersion(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
        version: Long,
    ): Boolean
}
