package no.novari.flyt.catalog.configuration.validation

class CouldNotFindIntegrationException(
    integrationId: Long,
) : RuntimeException("Could not find integration with id=$integrationId")
