package no.novari.flyt.catalog.configuration.validation

import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import no.novari.flyt.catalog.configuration.kafka.InstanceMetadataRequestProducerService
import no.novari.flyt.catalog.configuration.kafka.IntegrationRequestProducerService
import no.novari.flyt.catalog.configuration.kafka.MetadataRequestProducerService
import no.novari.flyt.catalog.configuration.model.integration.Integration
import no.novari.flyt.catalog.configuration.model.metadata.InstanceMetadataContent
import no.novari.flyt.catalog.configuration.model.metadata.InstanceValueMetadata
import no.novari.flyt.catalog.configuration.model.metadata.IntegrationMetadata
import org.hibernate.validator.HibernateValidatorFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.AbstractMap

@Service
class ConfigurationValidatorFactory(
    @Qualifier("configurationHibernateValidatorFactory") private val validatorFactory: ValidatorFactory,
    private val integrationRequestProducerService: IntegrationRequestProducerService,
    private val metadataRequestProducerService: MetadataRequestProducerService,
    private val instanceMetadataRequestProducerService: InstanceMetadataRequestProducerService,
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
        val integration: Integration =
            integrationRequestProducerService.get(integrationId)
                ?: throw CouldNotFindIntegrationException(integrationId)

        val metadata: IntegrationMetadata =
            metadataRequestProducerService.get(metadataId)
                ?: throw CouldNotFindMetadataException(metadataId)

        val instanceMetadata: InstanceMetadataContent =
            instanceMetadataRequestProducerService.get(metadataId)
                ?: throw CouldNotFindInstanceMetadataException(metadataId)

        return ConfigurationValidationContext
            .builder()
            .integration(integration)
            .metadata(metadata)
            .instanceValueTypePerKey(getInstanceValueTypePerKey(instanceMetadata))
            .build()
    }

    private fun getInstanceValueTypePerKey(
        instanceMetadata: InstanceMetadataContent,
    ): Map<String, InstanceValueMetadata.Type> =
        getInstanceValueTypePerKeyEntries(instanceMetadata)
            .associate { entry -> entry.key to entry.value }

    private fun getInstanceValueTypePerKeyEntries(
        instanceMetadataContent: InstanceMetadataContent,
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
