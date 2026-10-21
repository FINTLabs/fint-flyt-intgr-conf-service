package no.novari.flyt.catalog.integration.persistence

import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface IntegrationRepository : JpaRepository<Integration, Long> {
    @Query(
        """
        SELECT DISTINCT integration.sourceApplicationId
        FROM Integration integration
        WHERE integration.sourceApplicationId IS NOT NULL
        """,
    )
    fun findDistinctSourceApplicationIds(): Set<Long>

    fun existsIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
    ): Boolean

    fun findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
        sourceApplicationId: Long,
        sourceApplicationIntegrationId: String,
    ): Integration?

    fun findIntegrationsBySourceApplicationIdIn(sourceApplicationIds: Set<Long>): List<Integration>

    fun findIntegrationsBySourceApplicationIdIn(
        sourceApplicationIds: Set<Long>,
        pageable: Pageable,
    ): Page<Integration>
}
