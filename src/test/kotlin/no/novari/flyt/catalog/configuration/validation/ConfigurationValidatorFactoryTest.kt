package no.novari.flyt.catalog.configuration.validation

import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import no.novari.flyt.catalog.configuration.integrationDto
import no.novari.flyt.catalog.configuration.integrationMetadataDto
import no.novari.flyt.catalog.discovery.IntegrationMetadataService
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.integration.application.IntegrationService
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
    private lateinit var integrationService: IntegrationService
    private lateinit var integrationMetadataService: IntegrationMetadataService

    private lateinit var configurationValidatorFactory: ConfigurationValidatorFactory

    @BeforeEach
    fun setUp() {
        integrationService = mock()
        integrationMetadataService = mock()
        validatorFactory = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        configurationValidatorFactory =
            ConfigurationValidatorFactory(
                validatorFactory,
                integrationService,
                integrationMetadataService,
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
        whenever(integrationService.findById(any())).thenReturn(integrationDto())
        whenever(integrationMetadataService.getById(any()))
            .thenReturn(integrationMetadataDto(instanceMetadata = InstanceMetadataContentDto()))

        val result = configurationValidatorFactory.getValidator(1L, 1L)

        assertNotNull(result)
    }

    @Test
    fun `getValidator throws when the integration is not found`() {
        whenever(integrationService.findById(any())).thenReturn(null)

        assertThrows(CouldNotFindIntegrationException::class.java) {
            configurationValidatorFactory.getValidator(1L, 1L)
        }
    }

    @Test
    fun `getValidator throws when the metadata is not found`() {
        whenever(integrationService.findById(any())).thenReturn(integrationDto())
        whenever(integrationMetadataService.getById(any())).thenReturn(null)

        assertThrows(CouldNotFindMetadataException::class.java) {
            configurationValidatorFactory.getValidator(1L, 1L)
        }
    }

    @Test
    fun `getValidator throws when the instance metadata is not found`() {
        whenever(integrationService.findById(any())).thenReturn(integrationDto())
        whenever(integrationMetadataService.getById(any())).thenReturn(integrationMetadataDto(instanceMetadata = null))

        assertThrows(CouldNotFindInstanceMetadataException::class.java) {
            configurationValidatorFactory.getValidator(1L, 1L)
        }
    }
}
