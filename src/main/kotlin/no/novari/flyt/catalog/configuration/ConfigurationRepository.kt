package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import org.springframework.data.jpa.repository.JpaRepository

interface ConfigurationRepository : JpaRepository<Configuration, Long> {
    fun saveWithVersion(configuration: Configuration): Configuration {
        if (configuration.completed) {
            val nextVersion = getNextVersionForIntegrationId(requireNotNull(configuration.integrationId))
            configuration.version = nextVersion
        }
        return save(configuration)
    }

    fun getNextVersionForIntegrationId(integrationId: Long): Int =
        findFirstByIntegrationIdAndVersionNotNullOrderByVersionDesc(integrationId)
            ?.version
            ?.plus(1)
            ?: 1

    fun findFirstByIntegrationIdAndVersionNotNullOrderByVersionDesc(integrationId: Long): Configuration?
}
