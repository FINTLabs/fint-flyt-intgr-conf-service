package no.novari.flyt.catalog.valueconverting.infrastructure.persistence

import no.novari.flyt.catalog.valueconverting.domain.ValueConversion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface ValueConversionRepository :
    JpaRepository<ValueConversion, Long>,
    JpaSpecificationExecutor<ValueConversion> {
    @Query(
        """
        SELECT DISTINCT valueConversion.fromApplicationId
        FROM ValueConversion valueConversion
        WHERE valueConversion.fromApplicationId IS NOT NULL
        """,
    )
    fun findDistinctSourceApplicationIds(): Set<Long>
}
