package no.novari.flyt.catalog.configuration.validation

class CouldNotFindInstanceMetadataException(
    metadataId: Long,
) : RuntimeException("Could not find instance metadata for metadataId=$metadataId")
