package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.integrationDto
import no.novari.flyt.catalog.configuration.integrationMetadataDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidationContext
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IntegrationAndMetadataMatchesValidatorTest {
    private lateinit var validator: IntegrationAndMetadataMatchesValidator

    @BeforeEach
    fun setUp() {
        validator = IntegrationAndMetadataMatchesValidator()
    }

    @ParameterizedTest
    @CsvSource(
        "1,10,1,10,true",
        "1,10,2,10,false",
        "1,10,1,11,false",
        "1,10,2,11,false",
    )
    fun `accepts only when both the source application id and integration id match`(
        integrationSaId: Long,
        integrationSaiId: String,
        metadataSaId: Long,
        metadataSaiId: String,
        shouldBeValid: Boolean,
    ) {
        val context =
            ConfigurationValidationContext
                .builder()
                .integration(integrationDto(integrationSaId, integrationSaiId))
                .metadata(integrationMetadataDto(metadataSaId, metadataSaiId))
                .build()

        val hibernateContext: HibernateConstraintValidatorContext = mock()
        whenever(hibernateContext.getConstraintValidatorPayload(ConfigurationValidationContext::class.java))
            .thenReturn(context)

        val valid = validator.isValid(Configuration(), hibernateContext)

        assertEquals(shouldBeValid, valid)
    }
}
