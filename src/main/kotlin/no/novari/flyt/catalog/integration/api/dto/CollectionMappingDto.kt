package no.novari.flyt.catalog.integration.api.dto

data class CollectionMappingDto<T>(
    var elementMappings: MutableCollection<T> = mutableListOf(),
    var fromCollectionMappings: MutableCollection<FromCollectionMappingDto<T>> = mutableListOf(),
)
