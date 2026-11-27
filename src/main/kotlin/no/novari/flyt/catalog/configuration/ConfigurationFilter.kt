package no.novari.flyt.catalog.configuration

data class ConfigurationFilter(
    val integrationId: Long? = null,
    val completed: Boolean? = null,
    /** The integrations the user has access to. Null means no restriction. */
    val integrationIds: Set<Long>? = null,
)
