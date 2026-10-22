package no.novari.flyt.catalog.configuration.validation.instancereference

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import java.util.AbstractMap

interface InstanceValueTypeCompatibilityValidator {
    fun findIncompatibleInstanceValuesKeyAndType(
        valueMappingDto: ValueMappingDto,
        referencedInstanceValueKeys: Collection<String>,
        instanceValueTypePerKey: Map<String, InstanceValueMetadata.Type>,
    ): List<Map.Entry<String, InstanceValueMetadata.Type>> {
        if (valueMappingDto.type != getTypeToValidate()) {
            return emptyList()
        }

        return referencedInstanceValueKeys
            .filter { key -> !getCompatibleTypes().contains(instanceValueTypePerKey[key]) }
            .mapNotNull { key -> instanceValueTypePerKey[key]?.let { type -> AbstractMap.SimpleEntry(key, type) } }
    }

    fun getTypeToValidate(): ValueMapping.Type

    fun getCompatibleTypes(): Set<InstanceValueMetadata.Type>
}
