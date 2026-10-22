package no.novari.flyt.catalog.configuration.validation

class CouldNotFindMetadataException(
    metadataId: Long,
) : RuntimeException("Could not find metadata with id=$metadataId")
