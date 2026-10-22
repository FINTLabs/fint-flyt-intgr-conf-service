package no.novari.flyt.catalog.configuration.validation

import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import no.novari.flyt.catalog.discovery.IntegrationMetadataService
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import no.novari.flyt.catalog.integration.application.IntegrationService
import org.hibernate.validator.HibernateValidatorFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.AbstractMap

@Service
class ConfigurationValidatorFactory(
    @Qualifier("configurationHibernateValidatorFactory") private val validatorFactory: ValidatorFactory,
    private val integrationService: IntegrationService,
    private val integrationMetadataService: IntegrationMetadataService,
) {
    fun getValidator(
        integrationId: Long,
        metadataId: Long,
    ): Validator =
        validatorFactory
            .unwrap(HibernateValidatorFactory::class.java)
            .usingContext()
            .constraintValidatorPayload(getConfigurationValidationContext(integrationId, metadataId))
            .getValidator()

    private fun getConfigurationValidationContext(
        integrationId: Long,
        metadataId: Long,
    ): ConfigurationValidationContext {
        val integration =
            integrationService.findById(integrationId)
                ?: throw CouldNotFindIntegrationException(integrationId)

        val metadata =
            integrationMetadataService.getById(metadataId)
                ?: throw CouldNotFindMetadataException(metadataId)

        val instanceMetadata =
            metadata.instanceMetadata
                ?: throw CouldNotFindInstanceMetadataException(metadataId)

        return ConfigurationValidationContext
            .builder()
            .integration(integration)
            .metadata(metadata)
            .instanceValueTypePerKey(getInstanceValueTypePerKey(instanceMetadata))
            .build()
    }

    private fun getInstanceValueTypePerKey(
        instanceMetadata: InstanceMetadataContentDto,
    ): Map<String, InstanceValueMetadata.Type> =
        getInstanceValueTypePerKeyEntries(instanceMetadata)
            .associate { entry -> entry.key to entry.value }

    private fun getInstanceValueTypePerKeyEntries(
        instanceMetadataContent: InstanceMetadataContentDto,
    ): List<AbstractMap.SimpleEntry<String, InstanceValueMetadata.Type>> {
        val categoryEntries =
            instanceMetadataContent.categories
                .mapNotNull { it.content }
                .flatMap { getInstanceValueTypePerKeyEntries(it) }

        val valueEntries =
            instanceMetadataContent.instanceValueMetadata
                .mapNotNull { instanceValueMetadata ->
                    val key = instanceValueMetadata.key
                    val type = instanceValueMetadata.type
                    if (key == null || type == null) null else AbstractMap.SimpleEntry(key, type)
                }

        return categoryEntries + valueEntries
    }
}
