package no.novari.flyt.catalog.integration.api.dto

data class ObjectMappingDto(
    var valueMappingPerKey: MutableMap<String, ValueMappingDto> = mutableMapOf(),
    var valueCollectionMappingPerKey: MutableMap<String, CollectionMappingDto<ValueMappingDto>> = mutableMapOf(),
    var objectMappingPerKey: MutableMap<String, ObjectMappingDto> = mutableMapOf(),
    var objectCollectionMappingPerKey: MutableMap<String, CollectionMappingDto<ObjectMappingDto>> = mutableMapOf(),
)
