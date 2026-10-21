package no.novari.flyt.catalog.configuration.validation

import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import no.novari.flyt.catalog.configuration.kafka.InstanceMetadataRequestProducerService
import no.novari.flyt.catalog.configuration.kafka.IntegrationRequestProducerService
import no.novari.flyt.catalog.configuration.kafka.MetadataRequestProducerService
import no.novari.flyt.catalog.configuration.model.integration.Integration
import no.novari.flyt.catalog.configuration.model.metadata.InstanceMetadataContent
import no.novari.flyt.catalog.configuration.model.metadata.IntegrationMetadata
import org.hibernate.validator.HibernateValidatorContext
import org.hibernate.validator.HibernateValidatorFactory
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ConfigurationValidatorFactoryTest {
    private lateinit var validatorFactory: ValidatorFactory
    private lateinit var integrationRequestProducerService: IntegrationRequestProducerService
    private lateinit var metadataRequestProducerService: MetadataRequestProducerService
    private lateinit var instanceMetadataRequestProducerService: InstanceMetadataRequestProducerService

    private lateinit var configurationValidatorFactory: ConfigurationValidatorFactory

    @BeforeEach
    fun setUp() {
        integrationRequestProducerService =
            mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        metadataRequestProducerService =
            mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        instanceMetadataRequestProducerService =
            mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        validatorFactory = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        configurationValidatorFactory =
            ConfigurationValidatorFactory(
                validatorFactory,
                integrationRequestProducerService,
                metadataRequestProducerService,
                instanceMetadataRequestProducerService,
            )

        val hibernateValidatorFactory: HibernateValidatorFactory = mock()
        val hibernateValidatorContext: HibernateValidatorContext = mock()

        whenever(validatorFactory.unwrap(HibernateValidatorFactory::class.java)).thenReturn(hibernateValidatorFactory)
        whenever(hibernateValidatorFactory.usingContext()).thenReturn(hibernateValidatorContext)
        whenever(hibernateValidatorContext.constraintValidatorPayload(any<ConfigurationValidationContext>()))
            .thenReturn(hibernateValidatorContext)
        whenever(hibernateValidatorContext.validator).thenReturn(mock<Validator>())
    }

    @Test
    fun `getValidator builds a validator from integration and metadata context`() {
        val mockIntegration =
            Integration
                .builder()
                .sourceApplicationId(1L)
                .sourceApplicationIntegrationId("id")
                .destination("destination")
                .state(Integration.State.ACTIVE)
                .activeConfigurationId(100L)
                .build()

        val mockMetadata =
            IntegrationMetadata
                .builder()
                .sourceApplicationId(1L)
                .sourceApplicationIntegrationId("id")
                .sourceApplicationIntegrationUri("uri")
                .integrationDisplayName("name")
                .version(1L)
                .build()

        val mockInstanceMetadata =
            InstanceMetadataContent
                .builder()
                .categories(emptyList())
                .instanceObjectCollectionMetadata(emptyList())
                .instanceValueMetadata(emptyList())
                .build()

        whenever(integrationRequestProducerService.get(any())).thenReturn(mockIntegration)
        whenever(metadataRequestProducerService.get(any())).thenReturn(mockMetadata)
        whenever(instanceMetadataRequestProducerService.get(any())).thenReturn(mockInstanceMetadata)

        val result = configurationValidatorFactory.getValidator(1L, 1L)

        assertNotNull(result)
    }

    @Test
    fun `getValidator throws when the integration is not found`() {
        whenever(integrationRequestProducerService.get(any())).thenReturn(null)

        assertThrows(CouldNotFindIntegrationException::class.java) {
            configurationValidatorFactory.getValidator(1L, 1L)
        }
    }

    @Test
    fun `getValidator throws when the metadata is not found`() {
        val mockIntegration =
            Integration
                .builder()
                .sourceApplicationId(1L)
                .sourceApplicationIntegrationId("id")
                .destination("destination")
                .state(Integration.State.ACTIVE)
                .activeConfigurationId(100L)
                .build()

        whenever(integrationRequestProducerService.get(any())).thenReturn(mockIntegration)
        whenever(metadataRequestProducerService.get(any())).thenReturn(null)

        assertThrows(CouldNotFindMetadataException::class.java) {
            configurationValidatorFactory.getValidator(1L, 1L)
        }
    }

    @Test
    fun `getValidator throws when the instance metadata is not found`() {
        val mockIntegration =
            Integration
                .builder()
                .sourceApplicationId(1L)
                .sourceApplicationIntegrationId("id")
                .destination("destination")
                .state(Integration.State.ACTIVE)
                .activeConfigurationId(100L)
                .build()

        val mockMetadata =
            IntegrationMetadata
                .builder()
                .sourceApplicationId(1L)
                .sourceApplicationIntegrationId("id")
                .sourceApplicationIntegrationUri("uri")
                .integrationDisplayName("name")
                .version(1L)
                .build()

        whenever(integrationRequestProducerService.get(any())).thenReturn(mockIntegration)
        whenever(metadataRequestProducerService.get(any())).thenReturn(mockMetadata)
        whenever(instanceMetadataRequestProducerService.get(any())).thenReturn(null)

        assertThrows(CouldNotFindInstanceMetadataException::class.java) {
            configurationValidatorFactory.getValidator(1L, 1L)
        }
    }
}
