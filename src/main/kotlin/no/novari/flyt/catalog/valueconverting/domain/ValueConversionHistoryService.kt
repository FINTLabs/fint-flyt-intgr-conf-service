package no.novari.flyt.catalog.valueconverting.domain

import jakarta.persistence.EntityManager
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.audit.history.EnversHistoryService
import no.novari.flyt.catalog.valueconverting.api.dto.ValueConversionSnapshot
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ValueConversionHistoryService(
    @Qualifier("valueConvertingEntityManager") entityManager: EntityManager,
    displayResolver: ActorDisplayResolver,
    private val valueConversionMapper: ValueConversionMapper,
) : EnversHistoryService<ValueConversion, Long, ValueConversionSnapshot>(
        ValueConversion::class.java,
        entityManager,
        displayResolver,
    ) {
    public override fun mapSnapshot(entity: ValueConversion) = valueConversionMapper.toSnapshot(entity)
}
