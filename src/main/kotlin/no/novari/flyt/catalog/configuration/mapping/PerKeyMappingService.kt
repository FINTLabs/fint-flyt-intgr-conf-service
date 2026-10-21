package no.novari.flyt.catalog.configuration.mapping

import org.springframework.stereotype.Service

@Service
class PerKeyMappingService {
    fun <T, R> mapPerKey(
        existingMap: Map<String, T>,
        mapping: (T) -> R,
    ): Map<String, R> {
        return existingMap.mapValues { (_, value) -> mapping(value) }
    }
}
