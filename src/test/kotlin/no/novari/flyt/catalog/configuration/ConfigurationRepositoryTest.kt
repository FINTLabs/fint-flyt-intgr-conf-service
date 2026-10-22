package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ConfigurationRepositoryTest {
    private lateinit var configurationRepository: ConfigurationRepository
    private lateinit var configuration: Configuration

    @BeforeEach
    fun setUp() {
        configurationRepository = mock()
        configuration =
            Configuration
                .builder()
                .integrationId(1L)
                .integrationMetadataId(1L)
                .version(1)
                .completed(true)
                .build()
    }

    @Test
    fun `saveWithVersion assigns the next version when the configuration is completed`() {
        val integrationId = requireNotNull(configuration.integrationId)
        whenever(configurationRepository.saveWithVersion(configuration)).thenCallRealMethod()
        whenever(configurationRepository.getNextVersionForIntegrationId(integrationId)).thenReturn(2)
        whenever(configurationRepository.save(configuration)).thenReturn(configuration)

        val savedConfiguration = configurationRepository.saveWithVersion(configuration)

        assertEquals(2, savedConfiguration.version)
    }

    @Test
    fun `getNextVersionForIntegrationId increments the highest existing version`() {
        val integrationId = requireNotNull(configuration.integrationId)
        whenever(configurationRepository.getNextVersionForIntegrationId(integrationId)).thenCallRealMethod()
        whenever(configurationRepository.findFirstByIntegrationIdAndVersionNotNullOrderByVersionDesc(integrationId))
            .thenReturn(configuration)

        val nextVersion = configurationRepository.getNextVersionForIntegrationId(integrationId)

        assertEquals(2, nextVersion)
    }
}
