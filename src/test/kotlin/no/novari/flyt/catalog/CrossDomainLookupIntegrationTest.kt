package no.novari.flyt.catalog

import jakarta.validation.groups.Default
import no.novari.flyt.catalog.configuration.ConfigurationService
import no.novari.flyt.catalog.configuration.clearConfiguration
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidatorFactory
import no.novari.flyt.catalog.configuration.validation.constraints.IntegrationAndMetadataMatches
import no.novari.flyt.catalog.discovery.IntegrationMetadataService
import no.novari.flyt.catalog.discovery.clearDiscovery
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationPostDto
import no.novari.flyt.catalog.integration.application.IntegrationService
import no.novari.flyt.catalog.integration.application.IntegrationUpdateValidationService
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import no.novari.flyt.catalog.integration.web.UnprocessableEntityException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * Integration og configuration validerer mot hverandres data, og configuration mot discovery sine.
 * Testen kjører oppslagene mot ekte data i hvert sitt skjema, uten Kafka i mellom.
 */
class CrossDomainLookupIntegrationTest : CatalogIntegrationTest() {
    @Autowired
    lateinit var integrationService: IntegrationService

    @Autowired
    lateinit var integrationUpdateValidationService: IntegrationUpdateValidationService

    @Autowired
    lateinit var configurationService: ConfigurationService

    @Autowired
    lateinit var configurationValidatorFactory: ConfigurationValidatorFactory

    @Autowired
    lateinit var integrationMetadataService: IntegrationMetadataService

    @Autowired
    @Qualifier("integrationDataSource")
    lateinit var integrationDataSource: DataSource

    @Autowired
    @Qualifier("configurationDataSource")
    lateinit var configurationDataSource: DataSource

    @Autowired
    @Qualifier("discoveryDataSource")
    lateinit var discoveryDataSource: DataSource

    @AfterEach
    fun clearState() {
        JdbcTemplate(integrationDataSource).execute("truncate table integration restart identity cascade")
        JdbcTemplate(configurationDataSource).clearConfiguration()
        JdbcTemplate(discoveryDataSource).clearDiscovery()
    }

    @Test
    fun `configuration validation finds the integration and metadata it references`() {
        val integration = saveIntegration(sourceApplicationIntegrationId = "kildeapp-integrasjon")
        val metadata = saveMetadata(sourceApplicationIntegrationId = "kildeapp-integrasjon")

        val violations =
            configurationValidatorFactory
                .getValidator(integration.id, requireNotNull(metadata.id))
                .validate(configuration(integration.id, requireNotNull(metadata.id)), Default::class.java)

        assertThat(violations.map { it.constraintDescriptor.annotation.annotationClass })
            .doesNotContain(IntegrationAndMetadataMatches::class)
    }

    @Test
    fun `configuration validation rejects metadata that belongs to another integration`() {
        val integration = saveIntegration(sourceApplicationIntegrationId = "kildeapp-integrasjon")
        val metadata = saveMetadata(sourceApplicationIntegrationId = "en-annen-integrasjon")

        val violations =
            configurationValidatorFactory
                .getValidator(integration.id, requireNotNull(metadata.id))
                .validate(configuration(integration.id, requireNotNull(metadata.id)), Default::class.java)

        assertThat(violations.map { it.constraintDescriptor.annotation.annotationClass })
            .contains(IntegrationAndMetadataMatches::class)
    }

    @Test
    fun `activating an integration finds its completed configuration`() {
        val integration = saveIntegration()
        val configuration = configurationService.save(configuration(integration.id, 1L, completed = true))

        assertThatCode {
            integrationUpdateValidationService.validate(integration.id, integration.activatedWith(configuration))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `activating an integration rejects a configuration that belongs to another integration`() {
        val integration = saveIntegration()
        val otherIntegration = saveIntegration(sourceApplicationIntegrationId = "en-annen-integrasjon")
        val configuration = configurationService.save(configuration(otherIntegration.id, 1L, completed = true))

        assertThatThrownBy {
            integrationUpdateValidationService.validate(integration.id, integration.activatedWith(configuration))
        }.isInstanceOf(UnprocessableEntityException::class.java)
    }

    private fun saveIntegration(sourceApplicationIntegrationId: String = "kildeapp-integrasjon"): IntegrationDto =
        integrationService.save(
            IntegrationPostDto(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = sourceApplicationIntegrationId,
                destination = "arkiv",
            ),
        )

    private fun saveMetadata(sourceApplicationIntegrationId: String): IntegrationMetadataDto =
        integrationMetadataService.save(
            IntegrationMetadataDto(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = sourceApplicationIntegrationId,
                integrationDisplayName = "Byggesak",
                version = 1L,
                instanceMetadata = InstanceMetadataContentDto(),
            ),
        )

    private fun configuration(
        integrationId: Long,
        integrationMetadataId: Long,
        completed: Boolean = false,
    ): ConfigurationDto =
        ConfigurationDto(
            integrationId = integrationId,
            integrationMetadataId = integrationMetadataId,
            completed = completed,
            mapping = ObjectMappingDto(),
        )

    private fun IntegrationDto.activatedWith(configuration: ConfigurationDto): IntegrationDto =
        copy(state = Integration.State.ACTIVE, activeConfigurationId = configuration.id)
}
